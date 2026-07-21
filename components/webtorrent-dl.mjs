import { createHash } from 'crypto'
const WebTorrent = (await import('webtorrent')).default
const src = process.argv[2], dest = process.argv[3]
if (!src || !dest) { process.exit(1) }
const key = createHash('sha1').update(src).digest('hex').slice(0, 20)
const client = new WebTorrent({ maxConns: 200, uploads: 5, tracker: true, dht: true, utp: true, tcp: true })
let running = true
function die(c) { if (!running) return; running = false; client.destroy(() => process.exit(c)) }
const tor = client.add(src, { path: dest, name: key })
function r() { console.log(JSON.stringify({progress: tor.progress, downloaded: tor.downloaded, length: tor.length || 1, downloadSpeed: tor.downloadSpeed, numPeers: tor.numPeers, timeRemaining: tor.timeRemaining})) }
tor.on('download', r)
tor.on('done', () => { r(); die(0) })
tor.on('error', (e) => { console.error('err:', e.message); die(1) })
tor.on('warning', (e) => console.error('warn:', e.message))
setInterval(() => { if (tor.progress >= 1) die(0); r() }, 10000)
