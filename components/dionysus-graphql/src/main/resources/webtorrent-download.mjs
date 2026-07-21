const WebTorrent = (await import('webtorrent')).default

const source = process.argv[2]
const dest = process.argv[3]
if (!source || !dest) {
  console.error('Missing args')
  process.exit(1)
}

let running = true
const client = new WebTorrent({ maxConns: 55, tracker: true, dht: true, utp: true })

function die(code) {
  if (!running) return
  running = false
  client.destroy(() => process.exit(code))
}

const torrent = client.add(source, { path: dest })

function report() {
  console.log(JSON.stringify({
    progress: torrent.progress,
    downloaded: torrent.downloaded,
    length: torrent.length || 1,
    downloadSpeed: torrent.downloadSpeed,
    numPeers: torrent.numPeers,
    timeRemaining: torrent.timeRemaining,
  }))
}

torrent.on('infoHash', () => console.error('infoHash:', torrent.infoHash))
torrent.on('metadata', () => console.error('metadata:', torrent.name))
torrent.on('warning', (err) => console.error('warn:', err.message))
torrent.on('error', (err) => { console.error('err:', err.message); die(1) })
torrent.on('download', report)

torrent.on('done', () => {
  report()
  die(0)
})

setInterval(() => {
  if (torrent.progress >= 1) die(0)
  report()
  console.error('check: prog=' + (torrent.progress * 100).toFixed(1) + '% peers=' + torrent.numPeers + ' speed=' + (torrent.downloadSpeed / 1024).toFixed(0) + 'KB/s')
}, 10000)
