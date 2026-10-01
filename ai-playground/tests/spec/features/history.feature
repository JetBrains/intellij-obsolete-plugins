Feature: Chat History Management
  As a developer using the AI Playground
  I want comprehensive chat history management
  So that I can track and review my AI interactions

  Background:
    Given the AI Playground plugin is installed
    And I have configured valid API credentials
    And I have existing chat sessions

  Scenario: Chat History Persistence
    Given I have multiple chat sessions with:
      | session | messages | providers        |
      | Chat 1 | 5        | OpenAI           |
      | Chat 2 | 3        | Anthropic        |
      | Chat 3 | 4        | OpenAI,Anthropic |
    When I close and reopen the IDE
    Then all chat sessions should be restored
    And contain all original messages
    And maintain provider configurations
    And preserve token usage data

  Scenario: Session Token Tracking
    Given I am viewing a chat session
    Then I should see:
      | metric                    |
      | Total tokens used        |
      | Prompt tokens            |
      | Completion tokens        |
      | Cost estimate            |
    And metrics should be broken down by provider
    And show historical trends

  Scenario: Chat Search and Filtering
    Given I have multiple chat sessions
    When I search for "Python class"
    Then matching messages should be highlighted
    And I can filter by:
      | filter     |
      | Date range |
      | Provider   |
      | Model      |
      | Token usage|

  Scenario: Chat Export
    Given I have a chat session with multiple messages
    When I export the chat
    Then the export should include:
      | content            |
      | All messages      |
      | Timestamps        |
      | Provider details  |
      | Token usage       |
      | Model configs     |

  Scenario: Chat Cleanup
    Given I have old chat sessions
    When I run chat cleanup
    Then sessions should be archived based on:
      | criteria          |
      | Age              |
      | Usage frequency  |
      | Token count      |
      | Custom rules     |

  Scenario: History Navigation
    Given I have a long chat history
    When I navigate through messages
    Then I should see clear timestamps
    And provider information
    And token usage per message
    And be able to jump to specific points

  Scenario: Multi-Provider History
    Given I have chats with multiple providers
    When viewing the history
    Then messages should be clearly labeled by provider
    And show model configurations
    And indicate concurrent responses
    And maintain response relationships

  Scenario: Context Retention
    Given I have a chat with context from multiple files
    When I restore the chat session
    Then all context should be preserved
    And file references should be maintained
    And code snippets should be formatted

  Scenario: History Synchronization
    Given I have chat history on multiple devices
    When I open the IDE on a new device
    Then history should be synchronized
    And maintain consistency
    And resolve conflicts appropriately 