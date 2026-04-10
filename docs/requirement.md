# Urbanova Project Requirements

## Source Documents

- `Project Brief.docx`
- `Project Backlog1.docx`
- `marking scheme of CW2 for the joint school 2025.docx`

## 1. Project Overview

Urbanova is an application for hiring electric scooters in the city centre. The system must support both customer operations and management operations. The brief explicitly expects a client-server architecture, persistent data storage, automated testing, and an organised Scrum-like delivery process.

## 2. Product Goals

### 2.1 Customer-facing goals

Customers should be able to:

- register and log in to the system
- pay for a hire session or longer access period
- make a booking and select a specific scooter
- end or extend an active booking when supported
- track their own usage
- view scooter locations on a map
- view statistics about bookings, durations, and costs
- report issues or faults

### 2.2 Management-facing goals

Managers should be able to:

- estimate revenue based on registered customers
- review data for all registered customers
- handle and resolve user-reported issues or faults
- prioritise reported feedback
- configure scooter prices and costs
- offer discounts for frequent users

## 3. Functional Scope

The detailed functional scope is defined in the product backlog. The backlog currently contains 25 items:

- Priority 1: 8 items
- Priority 2: 15 items
- Priority 3: 2 items

The detailed list is recorded in `docs/backlog.md`.

## 4. Architecture and Technical Constraints

### 4.1 System architecture

- The solution must use a client-server architecture.
- The server may be monolithic or microservices-based.
- For the highest marks, the system should cope with simultaneous connections from several clients.

### 4.2 Data storage

- The server should interact with a database that stores domain data such as users, bookings, routes, and dates.
- SQL or NoSQL databases are preferred.
- File-based storage such as CSV or JSON is acceptable but will earn fewer marks.
- The database should be easy to run locally without special privileges or installation into system directories.
- The brief explicitly mentions SQLite as a suitable embedded option.

### 4.3 Required interfaces

Two interfaces are required:

- a customer interface
- a management interface

Allowed client forms:

- customer interface: desktop, web, or mobile
- management interface: desktop or web

### 4.4 Web client expectations

If the client is web-based, it should:

- be responsive
- be mobile friendly
- be accessible
- make non-trivial use of appropriate JavaScript libraries
- not rely only on plain HTML5 and CSS3

## 5. Testing, Build, and Deployment Expectations

### 5.1 Testing

- Testing must be part of development from the start.
- Testing should be automated where possible.
- The project documentation is expected to include a testing strategy.
- The documentation should include evidence of multiple test types.

### 5.2 Build and deployment

- Build, test, and deployment processes should be automated as much as possible.
- GitHub Actions is explicitly suggested as a useful option.
- The whole system should be buildable and runnable with a minimal number of commands.
- The application must be runnable locally for marking.
- Cloud deployment is optional, not required.
- A containerized version is optional extra credit.

## 6. Process Requirements

### 6.1 Development process

- The project follows a simplified Scrum process.
- Work is driven by a Product Owner (PO).
- The primary development period is split into three sprints.
- Each sprint should end with an MVP.

### 6.2 Required meetings

For each sprint, the team should have:

- one sprint planning meeting
- two or three short status meetings
- one sprint review meeting

Additional review expectations:

- Sprint 1 and Sprint 3: report to the PO face-to-face
- Sprint 2: receive peer feedback from two other groups and give feedback to two groups

### 6.3 Scrum Master responsibilities

The Scrum Master should:

- organise and chair planning, review, and status meetings
- nominate a note-taker for planning and review meetings
- ensure notes are uploaded to the project wiki
- maintain attendance records for all meetings in the wiki
- investigate absences and missed deadlines on behalf of the team

The brief also states:

- the Scrum Master is a facilitator, not the project manager
- the Scrum Master is also a developer
- the Scrum Master role should ideally rotate between sprints

### 6.4 Team member expectations

Team members are expected to:

- attend meetings or provide apologies for absences
- use the project issue tracker for tasks, bugs, and changes
- store other project documentation in the wiki
- use Git for all programming activities
- push local changes back to the remote repository regularly
- consider collaboration practices such as pair programming for complex or critical work

## 7. Repository, Issue Tracker, and Wiki Requirements

### 7.1 GitHub repository

- The team must use the allocated private GitHub repository.
- Every team member is expected to make regular commits during each sprint.
- The brief recommends agreeing on a Git workflow before Sprint 1.
- A feature-branch workflow is explicitly recommended over pushing directly to the main branch.
- One or more lead developers should handle merges and conflicts.

### 7.2 Issue tracker

The team should:

- define issue labels before recording work
- define milestones including `Sprint 1`, `Sprint 2`, `Sprint 3`, and `Final Demo`
- assign issues to the responsible team member
- close issues when the task is completed
- use the tracker for tasks, bugs, and feature changes

### 7.3 Wiki

The wiki is described as:

- the project's web site
- the home for project-related materials not stored in the repository
- the location for meeting notes and attendance records

## 8. Delivery-Oriented Requirement Summary

For this project, the team should ensure that the final solution includes:

- a runnable local client-server application
- customer and management interfaces
- persistent domain data storage
- a backlog-driven feature set
- automated testing with documented strategy and evidence
- documented requirements, design, and testing artifacts
- a maintained issue tracker, Git history, and project wiki

## 9. Important Requirement Notes and Possible Conflicts

### 9.1 Discount rule conflict

There is a mismatch between the brief and the backlog:

- `Project Brief.docx` gives an example discount for frequent users such as `6hrs a week`
- backlog item 22 specifies `8+hrs per week`, plus student and senior citizen discounts

For implementation, the backlog item should be treated as the more concrete requirement unless the PO says otherwise.

### 9.2 Sprint count vs demonstration count

There is also a process mismatch:

- the brief describes `three sprints`
- the marking scheme lists `sprint 1`, `sprint 2`, `sprint 3`, and `sprint 4` demonstrations

This likely means the final deliverable includes an additional final demonstration stage, but it should be confirmed with the teaching team if needed.
