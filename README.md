# slack-profile

A slack app that will be used to munge slack profiles so they show up specific ways in our partner channels. For now "certain ways" means with the users' company names appended to their profile names.

## todos
Slack workspaces can be set up to use display or "full names" for members. Partner is set to use full names. Consider changing back to "display name:" makes more sense that we can edit that instead of messing with users' real names.
Add more events (user join... others?)

## Installation
edit the manifest to include your slack oauth and validation tokens

Follow the steps for installing slacks apps, found here: https://api.slack.com/apps
You will need to use a "user token" for now. When slack stabilizes workspace tokens in the future they would be the way to go. The user who installs the app needs to be at a higher level than the users they will be editing.
Enable event subscriptions, subscribe to the user_change event
Add the user.profile.write and user.email.read scopes

edit the manifest to include your slack oauth and validation tokens
build and push the app to a PCF that has a legit non-self-signed cert or slack will reject it (PWS is a good choice).
