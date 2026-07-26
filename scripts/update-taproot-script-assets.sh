#!/bin/sh
set -eu

qa_assets_commit=6ea645d13b09bb5be644f154e50a9aa84f929ecb
qa_assets_sha256=cd789a58ec45916e1721cdd14e82ca4c93100959f1cef4e229b22e3bf539f095
selected_assets_sha256=4dd5ac8d3f5d1af52364ab52fd0823dfbd7dfb287872a9cdbd3dc581fe4c1df7
qa_assets_url="https://raw.githubusercontent.com/bitcoin-core/qa-assets/${qa_assets_commit}/unit_test_data/script_assets_test.json"
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
destination="${script_dir}/../src/test/resources/taproot-script-assets.json"
download=$(mktemp)
generated=$(mktemp)
trap 'rm -f "$download" "$generated"' EXIT

curl -fsSL "$qa_assets_url" -o "$download"
printf '%s  %s\n' "$qa_assets_sha256" "$download" | shasum -a 256 -c -

jq -c '
  def first($comment): map(select(.comment == $comment))[0];
  [
    first("siglen/empty_keypath"),
    first("sighash/keypath_hashtype_2"),
    first("spendpath/padshortcontrol"),
    first("opsuccess/bare"),
    first("opsuccess/exexecif"),
    first("sighash/keypath_unk_hashtype_76"),
    first("sighash/scriptpath_hashtype_0"),
    first("opsuccess/undecodable_bypass"),
    first("sighash/keypath"),
    first("applic/scriptpath"),
    first("sighash/purepk"),
    first("sighash/scriptpath_hashtype_82"),
    first("tapscript/checksigaddresults"),
    first("tapscript/checksigadd3args"),
    first("tapscript/checksigaddoversize"),
    first("tapscript/minimalif"),
    first("tapscript/minimalnotif"),
    first("tapscript/disabled_checkmultisig"),
    first("tapscript/disabled_checkmultisigverify"),
    (map(select(.comment == "tapscript/sigopsratio_0" and .final == true))[0]),
    first("tapscript/no10000limit"),
    first("tapscript/1000inputs"),
    first("tapscript/1000stack"),
    first("tapscript/input80limit"),
    first("tapscript/input81limit"),
    first("tapscript/inputmaxlimit"),
    first("tapscript/pushmaxlimit"),
    first("tapscript/cleanstack"),
    first("tapscript/emptysigs/checksig"),
    first("tapscript/emptysigs/checksigadd"),
    first("tapscript/emptysigs/nochecksigverify"),
    first("sighash/codesep_pk"),
    first("tapscript/unkpk/checksigadd"),
    first("inactive/scriptpath_valid_unkleaf"),
    first("tapscript/emptypk/checksig")
  ]
' "$download" > "$generated"

printf '%s  %s\n' "$selected_assets_sha256" "$generated" | shasum -a 256 -c -
chmod 644 "$generated"
mv "$generated" "$destination"
