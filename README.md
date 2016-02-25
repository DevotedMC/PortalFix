# PortalFix

**by Soerxpso**
for [Devoted](http://devotedmc.com)

Teleports players to natural spawn if they remain still for a given amount of time after using a portal.

##Config flags

**spawn_world** - The world to send players on stuck, using the MC world name
**recheck_time** - Time in seconds to wait after teleport or login before first checking if a player is still in a portal
**wait_time** - Time in seconds to wait until teleporting the player, if they seem stuck
**countdown_interval** - Time in seconds between alerts, sent to the player, instructing them to wait.
**teleport_message** - Message to send to the player if the first check after login or teleport indicates they are stuck. Use `&` to indicated a color or formatting code. `%0` is replaced with number of seconds until `wait_time`.
**countdown_message** - Alert message to send to players who are waiting out the countdown. `%0` is replaced with number of seconds left until `wait_time` is met.
**post\_teleport\_message** - Message to send to players after they have been teleported.
