const WebTorrent = (await import('webtorrent')).default

const source = process.argv[2]
const dest = process.argv[3]
if (!source || !dest) {
  process.exit(1)
}

const client = new WebTorrent()
const torrent = client.add(source, { path: dest })

torrent.on('download', () => {
  const data = {
    progress: torrent.progress,
    downloaded: torrent.downloaded,
    length: torrent.length,
    downloadSpeed: torrent.downloadSpeed,
    numPeers: torrent.numPeers,
    timeRemaining: torrent.timeRemaining,
  }
  console.log(JSON.stringify(data))
})

torrent.on('done', () => {
  const data = {
    progress: 1,
    downloaded: torrent.length,
    length: torrent.length,
    downloadSpeed: 0,
    numPeers: 0,
    timeRemaining: 0,
  }
  console.log(JSON.stringify(data))
  client.destroy(() => process.exit(0))
})

torrent.on('error', (err) => {
  console.error(err.message)
  client.destroy(() => process.exit(1))
})
