"""Gera o material da Saga compatível com a jornada proposta pelo Julio."""
from __future__ import annotations

import importlib.util
from pathlib import Path

from reportlab.lib.pagesizes import A4

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "doc/fase-4/fluxo-saga-proposta-julio.md"
OUTPUT = ROOT / "output/pdf/fluxo-saga-proposta-julio.pdf"

spec = importlib.util.spec_from_file_location(
    "proposal_layout", ROOT / "scripts/gerar-proposta-fase4.py")
layout = importlib.util.module_from_spec(spec)
spec.loader.exec_module(layout)


def make_table(rows, st):
    """Usa larguras adequadas ao conteúdo deste material."""
    header = " ".join(rows[0])
    count = len(rows[0])
    weights = [1] * count
    if count == 2:
        weights = [2.0, 3.0]
        if "Estado proposto" in header:
            weights = [1.7, 3.3]
    elif count == 3:
        weights = [1.3, 2.0, 1.7]
        if "Etapa" in header:
            weights = [.4, 2.45, 2.15]
        elif "Situação Tratamento" in header:
            weights = [1.55, 2.3, 1.15]
        elif "Passo" in header:
            weights = [.4, 1.05, 3.55]
        elif "Evento" in header:
            weights = [1.45, 1.55, 2.0]
    widths = [layout.WIDTH*w/sum(weights) for w in weights]
    data = [[layout.Paragraph(layout.inline(cell),
                             st["th" if i == 0 else "table"])
             for cell in row] for i, row in enumerate(rows)]
    table = layout.Table(data, colWidths=widths, repeatRows=1,
                         hAlign="LEFT", splitByRow=1, splitInRow=1)
    table.setStyle(layout.TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), layout.INK),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1),
         [layout.colors.white, layout.PALE]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("LINEBELOW", (0, 0), (-1, 0), .5, layout.INK),
        ("LINEBELOW", (0, 1), (-1, -1), .2, layout.LINE),
    ]))
    return table


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(layout.LINE)
    canvas.line(46, 39, A4[0]-46, 39)
    canvas.setFont("Body", 8)
    canvas.setFillColor(layout.MUTED)
    canvas.drawString(46, 25, "NumberOne | Saga do atendimento | Proposta para alinhamento")
    canvas.drawRightString(A4[0]-46, 25, str(doc.page))
    canvas.restoreState()


def main():
    layout.fonts()
    layout.make_table = make_table
    st = layout.styles()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    doc = layout.ProposalDoc(
        str(OUTPUT), pagesize=A4, leftMargin=46, rightMargin=46,
        topMargin=44, bottomMargin=54,
        title="NumberOne - Saga conforme jornada proposta pelo Julio",
        author="Equipe NumberOne", pageCompression=1)
    doc.build(layout.parse(SOURCE.read_text(encoding="utf-8"), st),
              onFirstPage=footer, onLaterPages=footer)
    print(OUTPUT)


if __name__ == "__main__":
    main()
