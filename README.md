# cicddollarai
<<<<<<< HEAD

Jenkins **Global Pipeline Library** source (Shared Library layout: `vars/*.groovy`).

- Default branch: **main**
- Jenkins loads this repo as library **`jenkinslibrary`** (see `jenkins-as-code/jenkins.yaml` in the `full cicd` workspace).

In `Jenkinsfile`:

```groovy
@Library('jenkinslibrary@main') _
```

Pass `credentialsId: 'github-pat'` into `gitCheckout([...])` when the application repo is private.
=======
cicddollarai
>>>>>>> 9b0beda258d0a18cdadd60c5100eb138b6cf91b6
