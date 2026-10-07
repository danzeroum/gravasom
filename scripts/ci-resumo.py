#!/usr/bin/env python3
"""Resume um artefato 'instrumentados-apiNN' extraido: testes, falhas e medidas.csv."""
import glob
import sys
import xml.etree.ElementTree as ET

raiz = sys.argv[1]
for xml in sorted(glob.glob(f"{raiz}/**/androidTest-results/connected/**/TEST-*.xml", recursive=True)):
    r = ET.parse(xml).getroot()
    print(f"== {r.attrib['tests']} testes, {r.attrib['failures']} falhas, {r.attrib.get('errors', 0)} erros")
    for tc in r.iter("testcase"):
        bad = tc.find("failure") if tc.find("failure") is not None else tc.find("error")
        print(("FALHOU" if bad is not None else "ok    "), tc.attrib["classname"].split(".")[-1], tc.attrib["name"], tc.attrib.get("time"))
        if bad is not None:
            print("     ", (bad.attrib.get("message") or bad.text or "")[:500].replace("\n", " | "))
for csv in sorted(glob.glob(f"{raiz}/**/tests-out/medidas.csv", recursive=True)):
    print("== medidas:", csv.split("tests-out")[0][-30:])
    print(open(csv).read())
