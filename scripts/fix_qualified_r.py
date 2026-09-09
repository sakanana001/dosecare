"""Replace R.string. with com.dosecare.app.R.string. in the 3 enum files."""
import re
for path in [
    'app/src/main/java/com/dosecare/app/domain/catalog/DrugCatalog.kt',
    'app/src/main/java/com/dosecare/app/domain/catalog/CypProfile.kt',
    'app/src/main/java/com/dosecare/app/domain/catalog/IndicationGroup.kt',
]:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = re.sub(r'(?<!com\.dosecare\.app\.)R\.string\.', 'com.dosecare.app.R.string.', content)
    if new_content != content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'{path}: fixed R. → com.dosecare.app.R.')
    else:
        print(f'{path}: no change')
