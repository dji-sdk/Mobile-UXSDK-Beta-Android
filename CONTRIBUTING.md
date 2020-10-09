Our aim is to continue to update the DJI UX SDK, making it easier and better to use. Further development of DJI UX SDK will now be done in this open sourced repository on GitHub, and we look forward to contributions from the community. We hope this document makes the process for contributing clear and answers some questions that you might have.

## Code of Conduct
We are committed to providing an inspiring and welcoming community for everyone. In order to do this, we expect everyone who participates to honor our [code of conduct](https://github.com/dji-sdk/Mobile-UXSDK-Beta-Android/blob/master/CODE_OF_CONDUCT.md).

## Development
The Android UX SDK team develops new features in an internal repository, which are released alongside with new MSDK releases. The Android UX SDK team is actively working on improving this process to bring over bug fixes and improvements to this open source repository as they are needed. 

External contributors can send pull requests for bugs and improvements, following the guidelines described in [Branch Organization](#branch-organization). For bigger changes like adding a feature or changing a public API, we recommend filing a [Github issue](https://github.com/dji-sdk/Mobile-UXSDK-Beta-Android/issues) which lets us reach an agreement on your proposal before you put significant effort into it. 

The core team members may add additional features at their discretion after an internal review. 

_**Note:** The core team is already working on the legacy widgets and panels that are present in the latest version of UXSDK 4.x._

## Code Style Guide
All contributors must follow this [style guide](https://github.com/dji-sdk/Mobile-UXSDK-Beta-Android/wiki/Code-Style-Guide).

## Branch Organization
We will keep the master branch up to date, with tests passing at all times. When making any changes, we recommend that you start with the latest version of the master branch.

If you send a pull request, please do it against the master branch. We maintain stable branches for major versions separately but we don’t accept pull requests to them directly. Instead, we cherry-pick non-breaking changes from master to the latest stable major version.

## Bugs
We are using [Github issues](https://github.com/dji-sdk/Mobile-UXSDK-Beta-Android/issues) for our public bugs. We keep a close eye on this and try to make it clear when we have an internal fix in progress. Before filing a new bug, try to make sure your problem doesn’t already exist. The best way to get your bug fixed is to provide a reduced test case to accurately reproduce the bug along with information about the DJI platform used and its firmware version, the Android device used and its OS version as well as specifics of the test (in-flight, during simulation of the aircraft etc).

## Pull Requests
If you haven't worked on pull requests before you can learn how from this free video series: 

[How to Contribute to an Open Source Project on GitHub](https://egghead.io/courses/how-to-contribute-to-an-open-source-project-on-github)

If you decide to fix an issue, please be sure to check the comment thread in case somebody is already working on a fix. If nobody is working on it at the moment, please leave a comment stating that you intend to work on it so other people don’t accidentally duplicate your effort. If somebody claims an issue but doesn’t follow up for more than two weeks, it’s fine to take it over, but you should still leave a comment.

The core team is monitoring for pull requests. We will review your pull request and either merge it, request changes to it, or close it with an explanation. For API changes we may need to fix our internal lower level code, which could cause some delay. We’ll do our best to provide updates and feedback throughout the process.

**Before submitting a pull request, please make sure the following is done:** 
1. Fork the repository and create your branch from master.
1. Format your code and make sure it follows the style guide.
1. Add tests if you’ve fixed a bug or added code that should be tested.
1. Ensure the test suite passes.

## Support
For any bugs or feature requests please use [Github issues](https://github.com/dji-sdk/Mobile-UXSDK-Beta-Android/issues) as mentioned above. 

You can send an email to dev@dji.com for any development support questions. You can also post questions, keep up to date on DJI developer news and contribute to the community by visiting the [DJI's Developer Forum here](https://forum.dji.com/forum-139-1.html?from=developer)

