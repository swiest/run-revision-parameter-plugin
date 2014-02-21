run-revision-parameter-plugin
=============================

This Jenkins plugin adds a new job parameter type 'Run Parameter, setting SCM revisions',
allowing use the same SCM revision of a previous run in an arbitrary job in the current build.

Currently, only Subversion and Git are supported as SCM types.

Conceptually, this is the reverse of the options "Pass-through Git Commit that was built"
or "Subversion Revision" for the [Parameterized Trigger](https://wiki.jenkins-ci.org/display/JENKINS/Parameterized+Trigger+Plugin)
plugin: Here, a job B looks up the revisions to be used from a specific build of job A.

