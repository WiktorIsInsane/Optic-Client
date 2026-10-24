import AdmZip from 'adm-zip'
import path from 'node:path'
import os from 'node:os'

const jar = path.join(
  os.homedir(),
  'AppData',
  'Roaming',
  '.lunar-remake',
  'versions',
  '1.0.0',
  'libs',
  'lunar-localpatches.jar'
)
const data = new AdmZip(jar)
  .getEntry('com/moonsworth/lunar/client/IRCIIHHICIHRCOCRROCOICRIHHCCHH/RCIRROCCCIIHCIHCCRHHCCHOHHHCHH.class')
  .getData()

const strings = []
for (let i = 0; i < data.length - 3; i++) {
  if (data[i] !== 1) continue
  const len = data.readUInt16BE(i + 1)
  if (len < 2 || len > 80 || i + 3 + len > data.length) continue
  const slice = data.subarray(i + 3, i + 3 + len)
  if (![...slice].every((b) => b >= 32 && b < 127)) continue
  const s = slice.toString('utf8')
  if (/lunar|client|optic|version|1\.8|\(v|satellite|locker|cosmetic|affiliated/i.test(s)) {
    strings.push({ at: i, len, s })
  }
}
for (const row of strings) console.log(JSON.stringify(row))
