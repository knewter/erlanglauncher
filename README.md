# Erlang <-> Android JInterface test

So this is a repo that shows that you can use erlang on android via jinterface
to talk to erlang on a remote node, easily enough (now that I've done all the
hard part).

There's a hard-coded IP address in the android app - you'll want to change that
to your android application's local network IP address.

Then locally, run (from this directory) erl, then:

```
c(hello_jinterface).
hello_jinterface:start().
```

Then run the android app.  You will see a ping/pong.  That's the successful
connection.  Huzzah!
