# jSnippets - make status updating easier

## history, reason for existence

Once upon a time I heard about something google did internally to make
status reporting easier, basically a small service that you'd send little
text updates -- aka "snippets" -- to (via cli, web ui, email, whatever) that'd 
be collated periodically for your manager to view in lieu of having to have a 
bunch of status update meetings. I've always thought that a sort of "status
twitter" would be potentially kind of a cool thing, even if using tech as a 
hammer for social problems like communicating effectively has a track record
that is, at best, mixed.

So that ended up turning into this Spring Boot service. I'll probably
split out a separate client repo at some point once I get the basic
web ui and rest api stood up.

## outstanding TODOs

- finish web ui
- finish rest api
- particularly, sort out api auth
- start email support (periodic reports? submission via email?)

## future work ideas

- integrate with ticket systems like jira?
- fancier web reporting ui to view updates along an org chart tree?

## run

- make jsnippets pgsql user and db it is owner of
- use env vars to set PG_HOST, PG_PORT, PG_DB, PG_USER, PG_PASS
- execute: java -jar path/to/jsnippets.jar

## run requirements

- jre 21
- postgresql (anything recent, say 10+)
- mail server (optional)

## build requirements

- jdk 21
- maven 3

## license

Apache 2.0, copyright authors as noted in banner.txt
