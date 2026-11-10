# Remote Config

Reads and changes Remote Config parameters (the `*_v2` flags, banners, tuning values) in dev or prod, through the REST API, without the console. It runs in Docker with one of `tools/firebase`'s gitignored admin keys, which picks the project:

```bash
./tools/firebase/remoteconfig/remote_config.sh admin-credentials-dev.json list            # every parameter
./tools/firebase/remoteconfig/remote_config.sh admin-credentials-dev.json list _v2        # names containing _v2
./tools/firebase/remoteconfig/remote_config.sh admin-credentials-prod.json get history_v2  # one, with its conditions
./tools/firebase/remoteconfig/remote_config.sh admin-credentials-prod.json set history_v2 --value true
./tools/firebase/remoteconfig/remote_config.sh admin-credentials-dev.json set week_tab_v2 --description "What it switches"
./tools/firebase/remoteconfig/remote_config.sh admin-credentials-dev.json set new_flag --value false --type BOOLEAN
./tools/firebase/remoteconfig/remote_config.sh admin-credentials-dev.json delete old_flag    # once no app version reads it
```

- **What `set` changes:** only the parameter's default value and description. Its conditional values and the rest of the template stay as they are, and a parameter that doesn't exist yet is added (`--type` sets its type, `STRING` otherwise).
- **What `delete` removes:** the whole parameter, its conditional values included. An app version that still reads it falls back to its in-app default, so delete a flag only once no released version depends on it.
- **History:** each `set` or `delete` publishes a new template version, so the console's history shows it and can roll it back. It fails rather than overwrite if someone published in between.
- **Prod is live:** a prod `set` reaches every user on their next fetch. Change dev first, and prod only when it's agreed.
- **The app:** reads its in-app defaults (`firebase_remote_config_defaults.xml`) until its first fetch. Debug builds fetch with no minimum interval, so relaunching twice shows a dev change on screen (see CLAUDE.md).
