# Sun VPS containers

Three LXD containers on `vps.scarlettparker.co.uk`:

| Name | IP | App |
| --- | --- | --- |
| `cassini` | `10.89.20.201` | Guided Reader |
| `artemis` | `10.89.20.202` | Sun backend, frontend, Postgres, Redis, Permify |
| `medusa` | `10.89.20.203` | Free Chess web and matchmaking |

## Commands

```sh
./vm/vps/vps.sh provision
./vm/vps/vps.sh status
./vm/vps/vps.sh backup
./vm/vps/vps.sh db-import
./vm/vps/vps.sh ssh-config
./vm/vps/vps.sh shell artemis

./vm/vps/deploy-sun.sh all
./vm/vps/deploy-sun.sh all --skip-build
```

`provision` is idempotent and is run by Woodpecker when `vm/vps/**` changes.
`deploy-sun.sh` deploys the Sun backend, frontend, and Permify config.

## SSH

`ssh-config` prints jump-host aliases:

```sh
ssh cassini
ssh artemis
ssh medusa
```

LXD also publishes host ports 2201-2203 as `cassini-port`, `artemis-port`, and
`medusa-port`. Those ports need to be allowed in the Oracle Cloud ingress rules
before they are reachable from outside.

## Data

`db-import` replaces the `artemis` `sun` database with a dump of the local Sun
database and resets the `hi@scarlettparker.co.uk` password. The new password is
written to `~/.local/share/sun-vps/hi-password`.

Daily backups run at 03:00 via `sun-vps-backup.timer` and land in
`/var/backups/sun-vps/daily`.

## Networking

`chess.scarlettparker.co.uk` is the Free Chess frontend. The matchmaking
websocket is published on `chess-server.scarlettparker.co.uk`.
