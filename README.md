# slack-profile

A slack app that can be used to munge slack profiles so they show up specific ways in our partner channels. For now "certain ways" means with the users' company names appended to their profile names.

## todos
* Slack workspaces can be set up to use "display names" or "full names" for members. The Partner workspace is set to use full names. This will need to be changed to "display name." This makes more sense: that we will be editing display names instead of messing with users' real names.
* Add more events (user join... others?)

## Installation
1. Follow the steps for installing slacks apps, found here: https://api.slack.com/apps. You will need to use a "user token" for now. When slack stabilizes workspace tokens in the future they would be the way to go. The user who installs the app needs to be at a higher security level than the users they will be editing.

2. Enable event subscriptions, subscribe to the user_change event

3. Add the user.profile.write and user.email.read scopes

4. Edit the manifest to include your slack oauth and validation tokens

5. Build and push the app to a PCF that has a legit non-self-signed cert or slack will reject it (PWS is a good choice). Currently it is running here:
```
api endpoint:   https://api.run.pivotal.io
api version:    2.116.0
user:           jgordon@pivotal.io
org:            platform-eng
space:          jgordon
profile-monger       started           1/1         1G       1G     profile-monger.cfapps.io
```

## How to use it
Once deployed the app will watch for profile updates in the workplace where it is installed. If a profile is updated it will check the user's display name and update it as configured by the application.

### see the suggested names
browse to https://profile-monger.cfapps.io/suggestions?token=verifcation_token

### to do a bulk upload
1. pull down and save the suggested names

```
curl 'https://profile-monger.cfapps.io/suggestions?token=verifcation_token' > names.json
```

2. review the names, looks good?

3. if so post the list

```
curl -d "@names.json" -H "Content-Type: application/json" -X POST 'https://profile-monger.cfapps.io/bulkUpdate?token=your-token&name_source=suggestedDisplayName' > results.json
```

4. review the results

5. no good? to rollback:

```
curl -d "@names.json" -H "Content-Type: application/json" -X POST 'https://profile-monger.cfapps.io/bulkUpdate?token=your-token&name_source=display_name' > results.json
```

