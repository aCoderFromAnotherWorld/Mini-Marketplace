Mini Marketplace

## GitHub Actions CI/CD

The project now includes a GitHub Actions workflow at `.github/workflows/ci-cd.yml`.

- Every push and pull request runs Maven tests, packages the Spring Boot app, and verifies the Docker image build.
- Pushes to `main` or `master` also publish the Docker image to GitHub Container Registry as `ghcr.io/<owner>/<repo>`.
- If you add the repository secret `RENDER_DEPLOY_HOOK_URL`, the workflow will trigger a Render deployment after the image is published.

### Optional secret

- `RENDER_DEPLOY_HOOK_URL`: Render deploy hook URL for automatic deployment after a successful main-branch pipeline.
