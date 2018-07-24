# slack-profile

A slack app that will be used to munge slack profiles so they show up specific ways in our partner channels. For now "certain ways" means with the users' company names appended to their profile names.

## todos
* Slack workspaces can be set up to use display or "full names" for members. Partner is set to use full names. Consider changing back to "display name:" makes more sense that we can edit that instead of messing with users' real names.
* Add more events (user join... others?)

## Installation
Edit the manifest to include your slack oauth and validation tokens

Follow the steps for installing slacks apps, found here: https://api.slack.com/apps
You will need to use a "user token" for now. When slack stabilizes workspace tokens in the future they would be the way to go. The user who installs the app needs to be at a higher level than the users they will be editing.

Enable event subscriptions, subscribe to the user_change event
Add the user.profile.write and user.email.read scopes

Edit the manifest to include your slack oauth and validation tokens
build and push the app to a PCF that has a legit non-self-signed cert or slack will reject it (PWS is a good choice).

## How to use it
Once deployed the app will watch for profile updates in the workplace where it is installed. If a profile is updated it will check the user's display name and update it as configured by the application.

### see the suggested names
browse to http://<app url>/suggestions?token=<verifcation_token>

### save the suggested names, for later rollback if needed
```curl http://<app url>/suggestions?token=<verifcation_token> > names.json```

### to do a bulk upload
1. pull down and save the suggested names

```curl http://<app url>/suggestions?token=<verifcation_token> > names.json```

2. review the names, looks good?

3. if so post the list

```curl -d "@names.json" -X POST http://<app url>/bulk?token=<verifcation_token>;source=suggestedDisplayName > results.json```

4. review the results

5. to rollback

```curl -d "@names.json" -X POST http://<app url>/bulk?token=<verifcation_token>;source=display_name > results.json```

