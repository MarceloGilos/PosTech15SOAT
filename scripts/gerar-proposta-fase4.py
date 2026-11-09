"""Exporta a proposta Markdown da equipe para PDF usando ReportLab."""
from __future__ import annotations

import html
import re
from pathlib import Path
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Flowable, KeepTogether, PageBreak, Paragraph, SimpleDocTemplate, Spacer,
    Table, TableStyle, XPreformatted,
)

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "doc/fase-4/proposta-equipe.md"
OUTPUT = ROOT / "output/pdf/proposta-equipe-fase-4.pdf"
WIDTH = A4[0] - 92
INK = colors.HexColor("#172D42")
BLUE = colors.HexColor("#206C89")
MUTED = colors.HexColor("#536779")
PALE = colors.HexColor("#F0F5F8")
LINE = colors.HexColor("#D5E0E7")


def fonts():
    folder = Path("C:/Windows/Fonts")
    for name, file in [("Body", "segoeui.ttf"), ("Body-Bold", "segoeuib.ttf"),
                       ("Body-Italic", "segoeuii.ttf"), ("Code", "consola.ttf")]:
        pdfmetrics.registerFont(TTFont(name, str(folder / file)))
    pdfmetrics.registerFontFamily("Body", normal="Body", bold="Body-Bold",
                                  italic="Body-Italic", boldItalic="Body-Bold")


def inline(value):
    # Guardar código antes de processar links e ênfase.
    saved = []

    def code(match):
        saved.append('<font name="Code" size="8">' + escape(match.group(1)) + '</font>')
        return f"ZZCODE{len(saved)-1}ZZ"

    value = re.sub(r"`([^`]+)`", code, value)
    value = escape(value)

    def link(match):
        label, url = match.group(1), html.unescape(match.group(2))
        if url.startswith(("https://", "http://")):
            return f'<link href="{escape(url)}" color="#206C89">{label}</link>'
        # Referências locais permanecem legíveis também numa cópia compartilhada.
        return label

    value = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", link, value)
    value = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", value)
    for i, item in enumerate(saved):
        value = value.replace(f"ZZCODE{i}ZZ", item)
    return value


class ProposalDoc(SimpleDocTemplate):
    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and hasattr(flowable, "outline_level"):
            key = f"heading-{self.seq.nextf('heading')}"
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(flowable.getPlainText(), key,
                                     flowable.outline_level, False)


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(LINE)
    canvas.line(46, 39, A4[0]-46, 39)
    canvas.setFont("Body", 8)
    canvas.setFillColor(MUTED)
    canvas.drawString(46, 25, "NumberOne  |  Proposta de implementação - Fase 4")
    canvas.drawRightString(A4[0]-46, 25, str(doc.page))
    canvas.restoreState()


class Architecture(Flowable):
    def __init__(self):
        super().__init__()
        self.width, self.height = WIDTH, 332

    def draw(self):
        c = self.canv
        gap = 12
        w = (WIDTH-2*gap)/3
        c.setFillColor(INK)
        c.roundRect(0, 291, WIDTH, 30, 5, fill=1, stroke=0)
        c.setFillColor(colors.white)
        c.setFont("Body-Bold", 10)
        c.drawCentredString(WIDTH/2, 302, "API Gateway + Lambda authorizer")
        c.setFillColor(MUTED)
        c.setFont("Body", 8.5)
        c.drawCentredString(WIDTH/2, 273, "VPC Link + ALB interno: entrada para os serviços no EKS")
        names = [("OS Service", "Diagnóstico / execução / Saga", "RDS PostgreSQL", "service_order_db"),
                 ("Billing Service", "Orçamento / Mercado Pago", "RDS PostgreSQL", "billing_db"),
                 ("Inventory Service", "Materiais / estoque", "RDS PostgreSQL", "inventory_db")]
        for i, (name, sub, db, dbname) in enumerate(names):
            x = i*(w+gap)
            c.setFillColor(INK)
            c.roundRect(x, 207, w, 45, 5, fill=1, stroke=0)
            c.setFillColor(colors.white)
            c.setFont("Body-Bold", 9)
            c.drawCentredString(x+w/2, 233, name)
            c.setFont("Body", 8)
            c.drawCentredString(x+w/2, 217, sub)
            c.setStrokeColor(BLUE)
            c.line(x+w/2, 207, x+w/2, 184)
            c.setFillColor(PALE)
            c.roundRect(x+3, 145, w-6, 39, 5, fill=1, stroke=0)
            c.setFillColor(INK)
            c.setFont("Body-Bold", 9)
            c.drawCentredString(x+w/2, 168, db)
            c.setFont("Code", 7.6)
            c.drawCentredString(x+w/2, 153, dbname)
            # Broker ligado ao participante, sem acesso aos bancos dos outros.
            c.setStrokeColor(BLUE)
            c.line(x+w-1, 207, x+w-1, 110)
        c.setFillColor(PALE)
        c.roundRect(0, 78, WIDTH, 32, 5, fill=1, stroke=0)
        c.setFillColor(BLUE)
        c.setFont("Body-Bold", 10)
        c.drawCentredString(WIDTH/2, 90, "Amazon SQS FIFO - comandos e resultados da Saga")
        c.setFillColor(PALE)
        c.roundRect(0, 12, WIDTH, 48, 5, fill=1, stroke=0)
        c.setFillColor(INK)
        c.setFont("Body-Bold", 10)
        c.drawCentredString(WIDTH/2, 42, "DynamoDB - histórico de revisões dos veículos")
        c.setFont("Body", 8.5)
        c.setFillColor(MUTED)
        c.drawCentredString(WIDTH/2, 25, "OS: projeção eventual via outbox e API de consulta; sem novo microsserviço.")


class Sequence(Flowable):
    def __init__(self, source):
        super().__init__()
        self.people, self.rows = [], []
        for line in source.splitlines():
            line = line.strip()
            match = re.match(r"participant (\w+) as (.+)", line)
            if match:
                self.people.append(match.groups())
            elif re.match(r"\w+->>\w+:", line):
                self.rows.append(("arrow", *re.match(r"(\w+)->>(\w+): (.+)", line).groups()))
            elif line.startswith("Note over "):
                target, text = line[len("Note over "):].split(":", 1)
                self.rows.append(("note", target, text.strip()))
        self.width = WIDTH
        self.height = 52 + sum(35 if row[0] == "note" else 27 for row in self.rows)

    def draw(self):
        c = self.canv
        positions = {key: (i+.5)*WIDTH/len(self.people)
                     for i, (key, label) in enumerate(self.people)}
        top = self.height - 32
        for key, label in self.people:
            x = positions[key]
            c.setFillColor(INK)
            c.roundRect(x-76, top, 152, 27, 4, fill=1, stroke=0)
            c.setFillColor(colors.white)
            c.setFont("Body-Bold", 8.3)
            c.drawCentredString(x, top+10, label)
            c.setStrokeColor(LINE)
            c.setDash(2, 3)
            c.line(x, top, x, 8)
            c.setDash()
        y = top - 10
        for row in self.rows:
            if row[0] == "arrow":
                _, a, b, label = row
                x1, x2 = positions[a], positions[b]
                y -= 23
                c.setStrokeColor(BLUE)
                c.line(x1, y, x2, y)
                sign = 1 if x2 > x1 else -1
                c.line(x2, y, x2-sign*5, y+3)
                c.line(x2, y, x2-sign*5, y-3)
                c.setFillColor(INK)
                c.setFont("Code", 8)
                c.drawCentredString((x1+x2)/2, y+5, label)
                y -= 4
            else:
                _, target, text = row
                y -= 35
                p = Paragraph(escape(text), ParagraphStyle(
                    "Note", fontName="Body", fontSize=8, leading=10,
                    textColor=MUTED, alignment=1))
                p.wrap(WIDTH-20, 30)
                c.setFillColor(PALE)
                c.roundRect(4, y-2, WIDTH-8, 29, 3, fill=1, stroke=0)
                p.drawOn(c, 10, y+4)


def styles():
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle("TitleClean", fontName="Body-Bold", fontSize=23,
                                leading=29, textColor=INK, spaceAfter=15),
        "h2": ParagraphStyle("Section", fontName="Body-Bold", fontSize=15,
                             leading=20, textColor=INK, spaceBefore=16,
                             spaceAfter=9, keepWithNext=True),
        "h3": ParagraphStyle("Subsection", fontName="Body-Bold", fontSize=11.5,
                             leading=16, textColor=BLUE, spaceBefore=12,
                             spaceAfter=7, keepWithNext=True),
        "body": ParagraphStyle("BodyClean", fontName="Body", fontSize=9.6,
                               leading=14.1, textColor=INK, spaceAfter=8,
                               allowWidows=0, allowOrphans=0),
        "list": ParagraphStyle("ListClean", fontName="Body", fontSize=9.3,
                               leading=13.7, textColor=INK, leftIndent=14,
                               firstLineIndent=0, bulletIndent=0, spaceAfter=6,
                               bulletFontName="Body", bulletFontSize=9.3,
                               allowWidows=0, allowOrphans=0),
        "table": ParagraphStyle("Cell", fontName="Body", fontSize=7.6,
                                leading=10.5, textColor=INK, splitLongWords=True),
        "th": ParagraphStyle("CellHeader", fontName="Body-Bold", fontSize=7.7,
                             leading=10.5, textColor=colors.white),
        "code": ParagraphStyle("CodeClean", fontName="Code", fontSize=7.8,
                               leading=10.4, textColor=INK, backColor=PALE,
                               borderPadding=9, spaceBefore=5, spaceAfter=11),
    }


def make_table(rows, st):
    count = len(rows[0])
    header = " ".join(rows[0])
    if count == 2:
        weights = [1.1, 3.9]
        if "Repositório" in header:
            weights = [1.7, 3.3]
    elif count == 3:
        weights = [1.2, 2.4, 1.4]
        if "Serviço / responsável" in header:
            weights = [1.2, 1.8, 2.0]
        elif "Hoje:" in header:
            weights = [1.2, 1.9, 1.9]
        elif "Fila FIFO" in header:
            weights = [2.2, .7, 2.1]
        elif "Operação local" in header:
            weights = [2.0, 1.4, 1.6]
        elif "Período" in header:
            weights = [.7, 1.9, 2.4]
        elif "Membro" in header:
            weights = [.65, 2.15, 2.2]
    elif count == 4:
        weights = [1.15, 1.25, 1.8, 1.3]
        if "Integrante" in header:
            weights = [.7, 1.1, 1.4, 1.8]
    elif count == 5:
        weights = [.35, 1.2, 2.1, .95, 1.2]
    else:
        weights = [1]*count
    widths = [WIDTH*w/sum(weights) for w in weights]
    data = [[Paragraph(inline(cell), st["th" if i == 0 else "table"])
             for cell in row] for i, row in enumerate(rows)]
    table = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT",
                  splitByRow=1, splitInRow=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), INK),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PALE]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("LINEBELOW", (0, 0), (-1, 0), .5, INK),
        ("LINEBELOW", (0, 1), (-1, -1), .2, LINE),
    ]))
    return table


def parse(text, st):
    lines = text.splitlines()
    story, i = [], 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue
        if line.startswith("```"):
            language = line[3:]
            block = []
            i += 1
            while i < len(lines) and not lines[i].strip().startswith("```"):
                block.append(lines[i])
                i += 1
            content = "\n".join(block)
            if language == "mermaid":
                story.append(Sequence(content) if "sequenceDiagram" in content else Architecture())
                story.append(Spacer(1, 10))
            else:
                longest = max((pdfmetrics.stringWidth(x, "Code", 7.8) for x in block), default=0)
                if longest > WIDTH-22:
                    raise ValueError(f"Linha de código excede largura ({longest:.1f}): {content}")
                story.append(KeepTogether([XPreformatted(escape(content), st["code"])]))
            i += 1
            continue
        heading = re.match(r"^(#{1,3}) (.+)$", line)
        if heading:
            level, content = len(heading.group(1)), heading.group(2)
            if content == "Dicionário para começar a leitura":
                story.append(Spacer(1, 42))
                story.append(Paragraph("Guia de leitura", st["h2"]))
                for text in [
                    "Comece pelo dicionário: os termos usados pela equipe estão reunidos antes da explicação do processo.",
                    "As seções 1 e 2 definem o escopo de três semanas, os serviços e os componentes AWS.",
                    "As seções 3 a 6 explicam antes/depois, reserva, compensação, SQS e transações por banco.",
                    "A seção 7 organiza as três semanas; a seção 8 reúne o checklist completo.",
                    "A seção 10 apresenta as entregas de Matheus, Julio, Anderson e Marcelo.",
                ]:
                    story.append(Paragraph(text, st["list"], bulletText="•"))
                story.append(Spacer(1, 28))
                story.append(Paragraph("Leitura técnica e planejamento da equipe", st["h3"]))
                story.append(Paragraph(
                    "Os exemplos descrevem a implementação proposta para a próxima etapa. "
                    "O checklist permite acompanhar a construção e reunir as evidências da entrega.",
                    st["body"]))
                story.append(PageBreak())
            p = Paragraph(inline(content), st["title" if level == 1 else f"h{level}"])
            if level in (2, 3):
                p.outline_level = level-2
            story.append(p)
            i += 1
            continue
        if line.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                cells = [x.strip() for x in lines[i].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-+:?", x) for x in cells):
                    rows.append(cells)
                i += 1
            table = make_table(rows, st)
            if len(rows) <= 5:
                headings = []
                while (story and isinstance(story[-1], Paragraph)
                       and hasattr(story[-1], "outline_level")):
                    headings.insert(0, story.pop())
                story.append(KeepTogether([*headings, table, Spacer(1, 10)]))
            else:
                story.extend([table, Spacer(1, 10)])
            continue
        bullet = re.match(r"^(?:- (\[ \] )?|(\d+)\. )(.+)$", line)
        if bullet:
            token = "□" if bullet.group(1) else (bullet.group(2)+"." if bullet.group(2) else "•")
            story.append(Paragraph(inline(bullet.group(3)), st["list"], bulletText=token))
            i += 1
            continue
        paragraph = [line]
        i += 1
        while i < len(lines) and lines[i].strip() and not re.match(r"^(#|\||```|- |\d+\. )", lines[i].strip()):
            paragraph.append(lines[i].strip())
            i += 1
        story.append(Paragraph(inline(" ".join(paragraph)), st["body"]))
    return story


def main():
    fonts()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    doc = ProposalDoc(str(OUTPUT), pagesize=A4, leftMargin=46, rightMargin=46,
                      topMargin=44, bottomMargin=54,
                      title="NumberOne - Proposta de implementação da Fase 4 na AWS",
                      author="Equipe NumberOne", pageCompression=1)
    doc.build(parse(SOURCE.read_text(encoding="utf-8"), styles()),
              onFirstPage=footer, onLaterPages=footer)
    print(OUTPUT)


if __name__ == "__main__":
    main()
