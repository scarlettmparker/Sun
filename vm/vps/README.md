# Sun VPS containers

LXD containers on `vps.scarlettparker.co.uk`.

```sh
./vm/vps/vps.sh provision
./vm/vps/vps.sh status
./vm/vps/vps.sh backup
./vm/vps/vps.sh db-import
./vm/vps/vps.sh ssh-config
./vm/vps/vps.sh shell cassini
```

Containers are defined in `containers.conf`. The provision command is idempotent
and is run by Woodpecker when `vm/vps/**` changes.

## SSH

`ssh-config` prints jump-host aliases:

```sh
ssh cassini   # 10.89.20.201 via vps
ssh artemis   # 10.89.20.202 via vps
ssh medusa    # 10.89.20.203 via vps
```

LXD also publishes host ports 2201-2203 as `cassini-port`, `artemis-port`, and
`medusa-port`. Those ports need to be allowed in the Oracle Cloud ingress rules
before they are reachable from outside.

## Data

`db-import` replaces the `artemis` `sun` database with a dump of the local Sun
database and resets the `hi@scarlettparker.co.uk` password. The new password is
written to `~/.local/share/sun-vps/hi-password`.

## Networking

- `cassini`: `10.89.20.201`
- `artemis`: `10.89.20.202`
- `medusa`: `10.89.20.203`

`chess.scarlettparker.co.uk` is the Free Chess frontend. The matchmaking
websocket is published separately on `chess-server.scarlettparker.co.uk`.
