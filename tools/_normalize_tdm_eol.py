"""把新建的 .kt 文件转成底座约定的 CRLF (无 BOM)。

底座 java/*.kt 全部是 CRLF + 无 BOM; `write` 工具落的是 LF, 所以新文件要归一化。
只处理本次新建的 3 个文件, 不碰任何既有文件。
"""
import io
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
FILES = [
    'app/src/main/java/com/dosecare/app/ui/tdm/TdmScreen.kt',
    'app/src/main/java/com/dosecare/app/ui/tdm/TdmViewModel.kt',
    'app/src/main/java/com/dosecare/app/ui/tdm/InteractiveConcentrationChart.kt',
]

for rel in FILES:
    p = os.path.join(ROOT, rel)
    data = io.open(p, 'rb').read()
    crlf = data.count(b'\r\n')
    lf = data.count(b'\n') - crlf
    if crlf == 0 and lf > 0:
        data = data.replace(b'\r\n', b'\n').replace(b'\n', b'\r\n')
        with io.open(p, 'wb') as f:
            f.write(data)
        print(f'  LF -> CRLF  {rel}')
    else:
        print(f'  SKIP (CRLF={crlf} LF={lf})  {rel}')
