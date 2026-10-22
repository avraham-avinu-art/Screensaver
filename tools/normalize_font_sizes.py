#!/usr/bin/env python3
"""Normalize undersized glyph outlines in Night Screen's custom fonts."""
from __future__ import annotations

import argparse
import io
import sys
import zipfile
from pathlib import Path
from typing import Dict, Tuple

from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.recordingPen import DecomposingPen
from fontTools.pens.t2CharStringPen import T2CharStringPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont
from fontTools.ttLib.tables.ttProgram import Program

MARKER = "GlyphSizeNormV1"

# Ratios were measured from the supplied files rendered at the same nominal size
# against Android Sans Serif reference metrics. Scaling is vertical only:
# horizontal advances and spacing remain unchanged.
FONT_FACTORS: Dict[str, Dict[str, float]] = {
    "Ashkenazi-Stam.ttf": {"digit": 1.460},
    "DL Adva.ttf": {"digit": 1.281, "hebrew": 1.154},
    "DL ApolloBlack.ttf": {"digit": 1.281},
    "DL Normador.TTF": {"digit": 1.141},
    "KeterAramTsova.ttf": {"digit": 1.106},
    "OzradCLM-Bold.otf": {"digit": 1.141},
    "ShofarRegular.ttf": {"digit": 1.177, "latin": 1.075},
    "ShortStack-Regular.ttf": {"digit": 1.074, "latin": 1.043},
    "fmhaimblack.ttf": {"digit": 1.237, "latin": 1.059},
    "gveretlevi-regular.ttf": {"digit": 1.159},
    "yehudaclm-light.ttf": {"digit": 1.217, "hebrew": 1.053},
}


def category(codepoint: int) -> str | None:
    if 0x30 <= codepoint <= 0x39:
        return "digit"
    if 0x0590 <= codepoint <= 0x05FF:
        return "hebrew"
    if 0x41 <= codepoint <= 0x5A or 0x61 <= codepoint <= 0x7A:
        return "latin"
    return None


def already_normalized(font: TTFont) -> bool:
    name_table = font.get("name")
    if name_table is None:
        return False
    for record in name_table.names:
        if record.nameID != 5:
            continue
        try:
            if MARKER in record.toUnicode():
                return True
        except Exception:
            continue
    return False


def factors_for_glyphs(font: TTFont, filename: str) -> Dict[str, float]:
    category_factors = FONT_FACTORS.get(filename, {})
    cmap = font.getBestCmap() or {}
    glyph_factors: Dict[str, float] = {}
    for codepoint, glyph_name in cmap.items():
        factor = category_factors.get(category(codepoint), 1.0)
        if factor > glyph_factors.get(glyph_name, 1.0):
            glyph_factors[glyph_name] = factor
    return {name: factor for name, factor in glyph_factors.items() if factor > 1.001}


def set_normalized_version(font: TTFont) -> None:
    if "name" in font:
        font["name"].setName("Version 1.0; " + MARKER, 5, 3, 1, 0x409)


def update_vertical_metrics(font: TTFont) -> None:
    """Ensure font-wide bounding boxes and metrics include the adjusted outlines."""
    if "glyf" in font:
        glyf = font["glyf"]
        bounds_done = {}
        for glyph_name in font.getGlyphOrder():
            try:
                glyf[glyph_name].recalcBounds(glyf, boundsDone=bounds_done)
            except Exception:
                continue
        bounds = [
            (getattr(glyph, "xMin", 0), getattr(glyph, "yMin", 0),
             getattr(glyph, "xMax", 0), getattr(glyph, "yMax", 0))
            for glyph in glyf.glyphs.values() if hasattr(glyph, "xMin")
        ]
        if not bounds:
            return
        font["head"].xMin = int(min(b[0] for b in bounds))
        font["head"].yMin = int(min(b[1] for b in bounds))
        font["head"].xMax = int(max(b[2] for b in bounds))
        font["head"].yMax = int(max(b[3] for b in bounds))
    elif "CFF " in font:
        glyph_set = font.getGlyphSet()
        bounds = []
        for glyph_name in glyph_set.keys():
            pen = BoundsPen(glyph_set)
            glyph_set[glyph_name].draw(pen)
            if pen.bounds:
                bounds.append(pen.bounds)
        if not bounds:
            return
        font["head"].xMin = int(min(b[0] for b in bounds))
        font["head"].yMin = int(min(b[1] for b in bounds))
        font["head"].xMax = int(max(b[2] for b in bounds))
        font["head"].yMax = int(max(b[3] for b in bounds))

    y_max = int(font["head"].yMax)
    y_min = int(font["head"].yMin)
    hhea = font["hhea"]
    hhea.ascent = max(int(hhea.ascent), y_max)
    hhea.descent = min(int(hhea.descent), y_min)
    if "OS/2" in font:
        os2 = font["OS/2"]
        os2.usWinAscent = min(65535, max(int(os2.usWinAscent), y_max, 0))
        os2.usWinDescent = min(65535, max(int(os2.usWinDescent), -y_min, 0))
        os2.sTypoAscender = max(int(os2.sTypoAscender), y_max)
        os2.sTypoDescender = min(int(os2.sTypoDescender), y_min)


def normalize_font_bytes(raw: bytes, filename: str) -> Tuple[bytes, bool, str]:
    source = TTFont(io.BytesIO(raw), recalcBBoxes=False, recalcTimestamp=False)
    if already_normalized(source):
        source.close()
        return raw, False, "already normalized"

    factors = factors_for_glyphs(source, filename)
    if not factors:
        source.close()
        return raw, False, "no size correction needed"

    output = TTFont(io.BytesIO(raw), recalcBBoxes=False, recalcTimestamp=False)
    factor_info = ", ".join(
        f"{group}×{factor:.3f}" for group, factor in FONT_FACTORS[filename].items()
    )

    if "glyf" in output:
        glyf = output["glyf"]
        source_glyph_set = source.getGlyphSet()

        # Decompose and scale selected composite glyphs from their original outlines,
        # so component shapes aren't accidentally scaled twice.
        for glyph_name, factor in factors.items():
            if glyf[glyph_name].isComposite():
                pen = TTGlyphPen(source_glyph_set)
                scaled_pen = TransformPen(pen, (1, 0, 0, factor, 0, 0))
                decomposing_pen = DecomposingPen(scaled_pen, source_glyph_set)
                source_glyph_set[glyph_name].draw(decomposing_pen)
                glyf[glyph_name] = pen.glyph()

        # Scale selected simple outlines around the baseline, preserving widths.
        for glyph_name, factor in factors.items():
            glyph = glyf[glyph_name]
            if glyph.isComposite():
                continue
            coordinates = getattr(glyph, "coordinates", None)
            if coordinates is None:
                continue
            for index, (x, y) in enumerate(coordinates):
                coordinates[index] = (x, int(round(y * factor)))
            glyph.coordinates = coordinates
            if hasattr(glyph, "program"):
                program = Program()
                program.fromBytecode([])
                glyph.program = program
            glyph.recalcBounds(glyf)

    elif "CFF " in output:
        cff = output["CFF "].cff
        top = cff.topDictIndex[0]
        glyph_set = output.getGlyphSet()
        for glyph_name, factor in factors.items():
            if glyph_name not in top.CharStrings.charStrings:
                continue
            advance_width = output["hmtx"].metrics.get(glyph_name, (0, 0))[0]
            pen = T2CharStringPen(advance_width, glyph_set)
            scaled_pen = TransformPen(pen, (1, 0, 0, factor, 0, 0))
            glyph_set[glyph_name].draw(scaled_pen)
            top.CharStrings[glyph_name] = pen.getCharString(
                private=top.Private, globalSubrs=cff.GlobalSubrs
            )
    else:
        source.close()
        output.close()
        return raw, False, "unsupported outline format"

    update_vertical_metrics(output)
    set_normalized_version(output)
    output.recalcTimestamp = False
    buffer = io.BytesIO()
    output.save(buffer)
    output.close()
    source.close()
    return buffer.getvalue(), True, factor_info


def normalize_assets(assets_dir: Path, zip_output: Path) -> None:
    if not assets_dir.is_dir():
        raise FileNotFoundError(f"Font asset directory not found: {assets_dir}")
    font_paths = sorted(
        path for path in assets_dir.iterdir()
        if path.is_file() and path.suffix.lower() in {".ttf", ".otf"}
    )
    if not font_paths:
        raise FileNotFoundError(f"No TTF/OTF fonts found in {assets_dir}")

    adjusted = 0
    for path in font_paths:
        raw = path.read_bytes()
        new_bytes, changed, info = normalize_font_bytes(raw, path.name)
        if changed:
            path.write_bytes(new_bytes)
            adjusted += 1
            print(f"Adjusted {path.name}: {info}")
        else:
            print(f"Kept {path.name}: {info}")

    zip_output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(
        zip_output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6
    ) as archive:
        for path in font_paths:
            archive.write(path, arcname=path.name)
    with zipfile.ZipFile(zip_output) as archive:
        bad_entry = archive.testzip()
        if bad_entry:
            raise zipfile.BadZipFile(f"Font archive validation failed at {bad_entry}")
    print(
        f"Validated {len(font_paths)} font files; adjusted {adjusted}; "
        f"archive={zip_output}"
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--assets-dir", required=True, type=Path)
    parser.add_argument("--zip-output", required=True, type=Path)
    args = parser.parse_args()
    normalize_assets(args.assets_dir, args.zip_output)
    return 0


if __name__ == "__main__":
    sys.exit(main())
