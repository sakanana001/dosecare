"""Add @StringRes displayNameRes field to each enum value."""
import re

# Mapping: (kotlin file path, enum name, prefix for string key)
ENUM_TARGETS = [
    ('app/src/main/java/com/dosecare/app/domain/catalog/DrugCatalog.kt', 'DrugCategory', 'drug_cat'),
    ('app/src/main/java/com/dosecare/app/domain/catalog/DrugCatalog.kt', 'OverdoseSeverity', 'sev'),
    ('app/src/main/java/com/dosecare/app/domain/catalog/CypProfile.kt', 'CypEnzyme', 'cyp'),
    ('app/src/main/java/com/dosecare/app/domain/catalog/CypProfile.kt', 'PathwayType', 'pathway'),
    ('app/src/main/java/com/dosecare/app/domain/catalog/IndicationGroup.kt', 'IndicationGroup', 'ind'),
    ('app/src/main/java/com/dosecare/app/domain/rules/Interactions.kt', 'Severity', 'severity'),
]

for path, enum_name, prefix in ENUM_TARGETS:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Match: enum class Foo(val displayName: String) { ... }
    # Replace with: enum class Foo(@StringRes val displayNameRes: Int, val displayName: String) { ... }
    pattern = rf'enum class {enum_name}\(val displayName: String\)'
    replacement = f'enum class {enum_name}(@androidx.annotation.StringRes val displayNameRes: Int, val displayName: String)'
    if not re.search(pattern, content):
        print(f'WARN: {path} :: {enum_name} signature not found')
        continue
    content = re.sub(pattern, replacement, content, count=1)

    # Replace R.string. with com.dosecare.app.R.string. since the enum is in a different package
    # (Only within the enum body context; we already have the body separately)
    # Actually do it globally in the file - simpler
    content = re.sub(r'\bR\.string\.', 'com.dosecare.app.R.string.', content)

    # Find the enum body and for each value, add ", R.string.<prefix>_<lowercase>" before the displayName string
    # Pattern: enum class X(...) { ... VALUE("displayName"), ... }
    # We need to convert VALUE("displayName") to VALUE(R.string.<prefix>_value, "displayName")
    # But we need to be careful — only inside the enum body, not other places.

    # Use the enum body block
    enum_match = re.search(rf'enum class {enum_name}.*?^\}}', content, re.DOTALL | re.MULTILINE)
    if not enum_match:
        print(f'WARN: {path} :: {enum_name} body not found')
        continue
    enum_body = enum_match.group(0)

    # For each value in enum_body like FOO("display"), insert ", R.string.<prefix>_foo" before the "display"
    def add_res(m):
        name = m.group(1)
        display = m.group(2)
        res_name = f'{prefix}_{name.lower()}'
        return f'{name}(R.string.{res_name}, "{display}")'

    # Pattern: NAME("display") inside enum body
    new_enum_body = re.sub(r'(\w+)\("([^"]*)"\)', add_res, enum_body)

    # Replace the enum body in content
    content = content.replace(enum_body, new_enum_body)

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'{path} :: {enum_name} updated')

print('Done')
