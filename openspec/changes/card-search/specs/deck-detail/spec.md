# Deck Detail Specification — Card Search

## Purpose

Enable in-memory card search within a deck detail screen, filtering cards by word, translation, and meaning fields.

## ADDED Requirements

### Requirement: Search State Management

The deck detail screen MUST maintain a `searchQuery` string in its UI state. A `SearchCardsChanged` intent SHALL update the query. The initial query SHALL be empty.

#### Scenario: Initial state has no search

- GIVEN the deck detail screen is opened
- WHEN the UI state is read
- THEN `searchQuery` is empty and all cards are displayed

#### Scenario: Query updated via intent

- GIVEN the deck detail screen is showing cards
- WHEN `SearchCardsChanged("hello")` is emitted
- THEN `searchQuery` becomes `"hello"` and filtered results are computed

### Requirement: Filter Logic

The system MUST filter cards using case-insensitive substring matching against `word`, `translation`, and `meaning` fields. The query SHALL be trimmed before matching. An empty or whitespace-only query SHALL return all cards.

#### Scenario: Match on word field

- GIVEN a deck contains a card with word `"Hello"`
- WHEN the search query is `"hel"`
- THEN the card is included in results

#### Scenario: Match on translation field

- GIVEN a deck contains a card with translation `"Hola"`
- WHEN the search query is `"ola"`
- THEN the card is included in results

#### Scenario: Match on meaning field

- GIVEN a deck contains a card with meaning `"Common greeting"`
- WHEN the search query is `"greet"`
- THEN the card is included in results

#### Scenario: Empty query shows all cards

- GIVEN the search query is `""`
- WHEN filter is applied
- THEN all cards in the deck are returned

#### Scenario: Whitespace-only query shows all cards

- GIVEN the search query is `"   "`
- WHEN filter is applied (after trimming)
- THEN all cards in the deck are returned

### Requirement: Search UI Components

The deck detail screen MUST display a search `TextField` with a leading search icon and a trailing clear button (visible only when query is non-empty). A filtered count badge SHALL show the number of matching cards.

#### Scenario: Search field with results

- GIVEN the search query matches 3 of 20 cards
- THEN the count badge displays `"3/20"`

#### Scenario: Clear button appears on non-empty query

- GIVEN the search query is `"test"`
- THEN the trailing clear button is visible
- WHEN the clear button is tapped
- THEN the query becomes empty and all cards are shown

### Requirement: No Results Empty State

When the search query is non-empty and yields zero matches, the screen SHALL display an empty state indicating no cards match the query.

#### Scenario: No matching cards

- GIVEN the search query is `"xyz"` and no card contains that substring
- THEN an empty state is shown with a message referencing the query

#### Scenario: Empty query never shows empty state

- GIVEN the search query is `""`
- THEN the normal card list is shown (never the no-results empty state)

### Requirement: Edge Case Handling

The search SHALL handle special characters and very long queries without crashing. Special characters in the query SHALL be treated as literal substring characters.

#### Scenario: Special characters in query

- GIVEN a card with word `"don't"`
- WHEN the search query is `"n'"`
- THEN the card is included in results

#### Scenario: Very long query

- GIVEN the search query exceeds 200 characters
- WHEN filter is applied
- THEN no crash occurs and results are computed normally (likely zero matches)
