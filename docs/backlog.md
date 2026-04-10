# Urbanova Product Backlog

## Source Documents

- `Project Backlog1.docx`
- `marking scheme of CW2 for the joint school 2025.docx`

## 1. Overview

The current project backlog contains 25 items. The marking scheme ties functionality marks directly to backlog priority:

- Priority 1: 8 items, 2 marks each, total 16 marks
- Priority 2: 15 items, 1 mark each, total 15 marks
- Priority 3: 2 items, 0.5 mark each, total 1 mark

Backlog items are tagged as:

- `F`: functional
- `NF`: non-functional

## 2. Backlog Summary Table

| ID | Type | Priority | Depends On | Original Description |
| --- | --- | --- | --- | --- |
| 1 | F | 1 | None | Support user accounts and user login |
| 2 | F | 2 | None | Option to store customer's card details for quicker bookings |
| 3 | NF | 2 | 2 | If ID 2: good security for user accounts |
| 4 | F | 1 | None | View hire options and cost: 1hr, 4hrs, 1day, 1week |
| 5 | F | 1 | None | Book an e-scooter; select e-scooter ID and hire period. |
| 6 | F | 1 | None | Handle card payment for booking (simulated) |
| 7 | F | 2 | None | Send booking confirmation via email |
| 8 | F | 1 | None | Store booking confirmation and display on demand |
| 9 | F | 2 | 7 | (Staff) Take bookings for unregistered users (req ID 7) |
| 10 | F | 2 | 5 | ID5: Update e-scooter status from available to unavailable |
| 11 | F | 2 | 5 | ID5: Option to extend current booking |
| 12 | F | 1 | None | Cancel booking |
| 13 | F | 2 | None | Send short feedback for issues/faults |
| 14 | F | 3 | 13 | if ID13: Prioritise feedback - escalate to high priority, resolve for low priority |
| 15 | F | 3 | 14 | if ID14: View high priority issues |
| 16 | F | 1 | None | Configure e-scooter details and costs. |
| 17 | F | 2 | None | Display scooter list availability: availability/location if available |
| 18 | F | 2 | None | Display the five scooter locations on a visual map (see sheet 2) |
| 19 | F | 1 | None | View weekly income for rental options: 1hr, 4hr, day, week (tracking popular hire length) |
| 20 | F | 2 | None | View combined daily income over a week duration (tracking popular hire days- include 1hr, 4hr, 1day, discount 1week hire period in this statistic) |
| 21 | F | 2 | 19, 20 | If ID 19, 20: plot weekly income graphically |
| 22 | F | 2 | None | Discount applied for frequent users (8+hrs per week), students, senior citizens |
| 23 | F | 2 | None | Support usage by multiple clients simultaneously |
| 24 | NF | 2 | None | Provide a responsive user interface |
| 25 | NF | 2 | None | Address issues of accessibility (colour & font choices, etc) |

## 3. Detailed Backlog Items

### ID 1

- Original description: `Support user accounts and user login`
- Type: `F`
- Priority: `1`
- Depends on: `None`

### ID 2

- Original description: `Option to store customer's card details for quicker bookings`
- Type: `F`
- Priority: `2`
- Depends on: `None`

### ID 3

- Original description: `If ID 2: good security for user accounts`
- Type: `NF`
- Priority: `2`
- Depends on: `ID 2`

### ID 4

- Original description: `View hire options and cost: 1hr, 4hrs, 1day, 1week`
- Type: `F`
- Priority: `1`
- Depends on: `None`

### ID 5

- Original description: `Book an e-scooter; select e-scooter ID and hire period.`
- Type: `F`
- Priority: `1`
- Depends on: `None`

### ID 6

- Original description: `Handle card payment for booking (simulated)`
- Type: `F`
- Priority: `1`
- Depends on: `None`

### ID 7

- Original description: `Send booking confirmation via email`
- Type: `F`
- Priority: `2`
- Depends on: `None`

### ID 8

- Original description: `Store booking confirmation and display on demand`
- Type: `F`
- Priority: `1`
- Depends on: `None`

### ID 9

- Original description: `(Staff) Take bookings for unregistered users (req ID 7)`
- Type: `F`
- Priority: `2`
- Depends on: `ID 7`

### ID 10

- Original description: `ID5: Update e-scooter status from available to unavailable`
- Type: `F`
- Priority: `2`
- Depends on: `ID 5`

### ID 11

- Original description: `ID5: Option to extend current booking`
- Type: `F`
- Priority: `2`
- Depends on: `ID 5`

### ID 12

- Original description: `Cancel booking`
- Type: `F`
- Priority: `1`
- Depends on: `None`

### ID 13

- Original description: `Send short feedback for issues/faults`
- Type: `F`
- Priority: `2`
- Depends on: `None`

### ID 14

- Original description: `if ID13: Prioritise feedback - escalate to high priority, resolve for low priority`
- Type: `F`
- Priority: `3`
- Depends on: `ID 13`

### ID 15

- Original description: `if ID14: View high priority issues`
- Type: `F`
- Priority: `3`
- Depends on: `ID 14`

### ID 16

- Original description: `Configure e-scooter details and costs.`
- Type: `F`
- Priority: `1`
- Depends on: `None`

### ID 17

- Original description: `Display scooter list availability: availability/location if available`
- Type: `F`
- Priority: `2`
- Depends on: `None`

### ID 18

- Original description: `Display the five scooter locations on a visual map (see sheet 2)`
- Type: `F`
- Priority: `2`
- Depends on: `None`

### ID 19

- Original description: `View weekly income for rental options: 1hr, 4hr, day, week (tracking popular hire length)`
- Type: `F`
- Priority: `1`
- Depends on: `None`

### ID 20

- Original description: `View combined daily income over a week duration (tracking popular hire days- include 1hr, 4hr, 1day, discount 1week hire period in this statistic)`
- Type: `F`
- Priority: `2`
- Depends on: `None`

### ID 21

- Original description: `If ID 19, 20: plot weekly income graphically`
- Type: `F`
- Priority: `2`
- Depends on: `ID 19, ID 20`

### ID 22

- Original description: `Discount applied for frequent users (8+hrs per week), students, senior citizens`
- Type: `F`
- Priority: `2`
- Depends on: `None`

### ID 23

- Original description: `Support usage by multiple clients simultaneously`
- Type: `F`
- Priority: `2`
- Depends on: `None`

### ID 24

- Original description: `Provide a responsive user interface`
- Type: `NF`
- Priority: `2`
- Depends on: `None`

### ID 25

- Original description: `Address issues of accessibility (colour & font choices, etc)`
- Type: `NF`
- Priority: `2`
- Depends on: `None`

## 4. Suggested Feature Grouping

This grouping is an organisational aid only. The authoritative requirement remains the original backlog wording above.

### 4.1 Customer booking flow

- ID 1: accounts and login
- ID 4: hire options and cost
- ID 5: booking flow
- ID 6: simulated payment
- ID 8: booking confirmation storage and display
- ID 10: scooter availability status update
- ID 11: booking extension
- ID 12: cancellation
- ID 22: discounts

### 4.2 Customer support and communication

- ID 7: email confirmation
- ID 13: issue or fault feedback
- ID 14: feedback prioritisation
- ID 15: high-priority issue view

### 4.3 Operations and management

- ID 9: staff booking for unregistered users
- ID 16: configure scooter details and costs
- ID 19: weekly income by hire option
- ID 20: combined daily income over a week
- ID 21: graphical income view

### 4.4 Discovery and UI quality

- ID 17: scooter availability listing
- ID 18: map view of scooter locations
- ID 23: multi-client support
- ID 24: responsive UI
- ID 25: accessibility

## 5. Notes

- The backlog file calls items `functional (F)` or `non-functional (N)`, but the table itself uses `NF` for non-functional items. This document preserves the table values.
- Dependency relationships in this document are taken directly from wording such as `If ID 2` or `ID5`.
