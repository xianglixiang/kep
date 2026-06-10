"""Generate a 9-feature coverage test docx for the M2-PoC conversion evaluation.

Features covered (per design §2):
  1. Headings (H1/H2/H3)
  2. Bold/italic/underline/color
  3. Nested bullet + numbered lists (2 levels)
  4. 3x4 table with merged cells
  5. Embedded PNG image (1x1 transparent)
  6. Hyperlink
  7. Footnote (placeholder — python-docx doesn't support directly)
  8. Monospace font (Courier New) for code block simulation
  9. Page header text
"""
from docx import Document
from docx.shared import Inches, RGBColor
import io


def build() -> None:
    doc = Document()

    # 1. Headings
    doc.add_heading('一级标题：测试文档', level=1)
    doc.add_heading('二级标题：保真度评估', level=2)
    doc.add_heading('三级标题：M2 PoC', level=3)

    # 2. Inline styles
    p = doc.add_paragraph('行内样式：')
    p.add_run('加粗 ').bold = True
    p.add_run('斜体 ').italic = True
    p.add_run('下划线 ').underline = True
    p.add_run('彩色').font.color.rgb = RGBColor(0xC0, 0x00, 0x00)

    # 3. Nested lists (2 levels)
    doc.add_paragraph('无序列表项 1', style='List Bullet')
    doc.add_paragraph('无序列表项 2', style='List Bullet')
    sub = doc.add_paragraph('嵌套 1', style='List Bullet')
    sub.paragraph_format.left_indent = Inches(0.5)
    doc.add_paragraph('有序 1', style='List Number')
    doc.add_paragraph('有序 2', style='List Number')

    # 4. 3x4 table with merged cells
    table = doc.add_table(rows=3, cols=4)
    table.style = 'Table Grid'
    table.cell(0, 0).text = '表头1'
    table.cell(0, 1).text = '表头2'
    table.cell(0, 2).text = '表头3'
    table.cell(0, 3).text = '表头4'
    table.cell(1, 0).merge(table.cell(1, 1)).text = '合并 A+B'
    table.cell(1, 2).text = 'C'
    table.cell(1, 3).text = 'D'
    table.cell(2, 0).text = 'A'
    table.cell(2, 1).text = 'B'
    table.cell(2, 2).merge(table.cell(2, 3)).text = '合并 C+D'

    # 5. Embedded PNG (1x1 transparent)
    png_bytes = bytes.fromhex(
        '89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C489'
        '0000000A49444154789C63000100000500010D0A2DB40000000049454E44AE426082'
    )
    doc.add_picture(io.BytesIO(png_bytes), width=Inches(2))

    # 6. Hyperlink
    doc.add_paragraph('访问 https://example.com 了解更多信息')

    # 7. Footnote — python-docx 不直接支持,跳过
    # (LibreOffice 仍会尝试渲染, mammoth 多数情况会忽略)

    # 8. Monospace font (code block simulation)
    code = doc.add_paragraph('def hello(): return "world"')
    code.runs[0].font.name = 'Courier New'

    # 9. Page header
    section = doc.sections[0]
    header = section.header
    header.paragraphs[0].text = 'KEP M2-PoC 测试文档'

    doc.save('fixtures/test-doc.docx')
    print('生成 fixtures/test-doc.docx OK')


if __name__ == '__main__':
    build()
