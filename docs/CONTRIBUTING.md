# Contributing to Urbanova

Thank you for contributing to Urbanova.  
This project follows a structured Scrum-based workflow using GitHub Issues, Projects (Kanban), and Pull Requests.

Please read the guidelines below before making any changes.

---

## 1. Branching Strategy

We follow a Feature Branch Workflow.

Rules:

- Never push directly to `main`
- All changes must be made in a feature branch
- All merges must go through a Pull Request
- `main` must always remain stable and runnable

Branch naming convention:

feature/<short-description>  
bugfix/<short-description>  
docs/<short-description>  

Examples:

feature/login-auth  
feature/booking-logic  
bugfix/cancel-error  
docs/update-readme  

---

## 2. Development Workflow

Before starting work:

1. Pull latest main  
   git checkout main  
   git pull origin main  

2. Create your feature branch  
   git checkout -b feature/<name>  

3. Implement your changes  

4. Commit using proper commit message format  

5. Push branch  
   git push origin feature/<name>  

6. Create Pull Request  

---

## 3. Commit Message Format

We follow a structured commit format:

type(scope): short description

Common types:

feat – new feature  
fix – bug fix  
docs – documentation changes  
test – testing updates  
refactor – code restructuring  
chore – configuration or minor maintenance  

Examples:

feat(auth): implement login endpoint  
fix(booking): prevent duplicate cancellation  
docs(wiki): update sprint plan  

Do NOT use vague messages like:
update
fix bug
changes

---

## 4. Pull Request Rules

Every Pull Request must:

- Be linked to an Issue (use: Closes #IssueNumber)
- Contain a clear description of what was implemented
- Be reviewed before merging
- Pass basic functional testing

The Lead Developer is responsible for reviewing and merging PRs.

---

## 5. Issue Management

All work must begin with an Issue.

Issue requirements:

- Clear description
- Assigned to a team member
- Linked to a Milestone (Sprint 1, Sprint 2, etc.)

Workflow:

Backlog → Ready → In progress → In review → Done

Issues must be closed only after PR is merged.

---

## 6. Code Review & Merge Policy

- No direct merge to main
- At least one review required
- PR must not break existing functionality
- Large PRs should be avoided

Keep PRs small and focused.

---

## 7. Testing Requirements

Before marking an Issue as Done:

- Feature must work as expected
- Basic manual testing completed
- No major console errors
- Related documentation updated

Testing is coordinated by the Testing & Documentation role.

---

## 8. Code Quality Expectations

- Follow project structure
- Keep controller, service, repository layers separated
- Avoid hardcoding credentials
- Write readable and maintainable code

---

By contributing to this project, you agree to follow these guidelines.