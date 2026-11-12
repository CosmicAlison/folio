"""Validate render.yaml against Render's current public schema (CI tooling only)."""
import json
from pathlib import Path
from urllib.request import urlopen

import jsonschema
import yaml

with urlopen('https://render.com/schema/render.yaml.json', timeout=30) as response:
    schema = json.load(response)
blueprint = yaml.safe_load(Path('render.yaml').read_text())
jsonschema.Draft202012Validator(schema).validate(blueprint)
print('Render Blueprint schema validation passed')
