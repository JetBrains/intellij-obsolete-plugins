Feature: Chat Functionality
  As a developer using the AI Playground
  I want to interact with LLMs through a chat interface
  So that I can get AI assistance while developing

  Background:
    Given the AI Playground plugin is installed
    And I have configured valid API credentials

  Scenario: Single Provider Chat
    Given a provider is configured with a model
    When I send a message "Hello, how can you help me with Python?"
    Then I should see a streaming response from the provider
    And the response should be relevant to Python development
    And both message and response should be saved in history

  Scenario: Multi-Provider Chat
    Given I have configured the following providers:
      | provider  | model      |
      | OpenAI   | gpt-4      |
      | Anthropic| claude-2   |
    When I send a message "Compare list vs tuple in Python"
    Then all providers should respond concurrently
    And responses should stream in real-time
    And responses should be displayed side by side
    And I should see clear provider labels

  Scenario: Chat History Persistence
    Given I have multiple chat sessions with different messages
    When I close and reopen the IDE
    Then all chat histories should be preserved
    And include original messages and responses
    And show correct timestamps
    And display token usage information

  Scenario: Chat Session Management
    Given I have multiple active chat sessions
    When I switch between chat sessions
    Then each session should load its complete history
    And maintain its provider configurations
    And show correct token usage statistics

  Scenario: Message Regeneration
    Given I have received a response in chat
    When I request to regenerate the response
    Then the provider should generate a new response
    And both responses should be preserved in history
    And token usage should be tracked for both

  Scenario: Response Streaming
    Given I have a chat open
    When a provider is generating a response
    Then the response should stream in the window
    And maintain proper formatting
    And allow interrupting the response

  Scenario: Error Recovery
    Given I am chatting with multiple providers
    When one provider encounters an error
    Then other providers should continue responding
    And I should see an error message for the failed provider
    And have an option to retry the failed request

  Scenario: Token Usage Tracking
    Given I am having a chat conversation
    When messages are exchanged
    Then token usage should be tracked per message
    And total usage should be visible
    And usage should be tracked per provider

  Scenario: Chat Context Management
    Given I have a chat open
    When I send messages with code context
    Then the chat should maintain context between messages
    And show relevant code snippets
    And preserve formatting of code blocks
    And respect context window limits

  Scenario: Opening Chat in Editor
    Given I have an existing chat session
    When I open the chat
    Then it should open in an editor window
    And display the complete chat history
    And show the chat input field
    And maintain all provider configurations

  Scenario: Multiple Chats
    Given I have multiple chat sessions
    When I open each chat
    Then each chat should open in its own window
    And I can switch between chats
    And each window maintains its own state
    And each window shows correct chat history

  Scenario: Preventing Duplicate Editors
    Given I have a chat open in an editor
    When I try to open the same chat again
    Then no new editor should be created
    And focus should move to the existing editor
    And the existing editor should maintain its state

  Scenario: Chat State Persistence
    Given I have multiple chats open
    When I close and reopen the IDE
    Then all previously open chats should reopen
    And maintain their previous state
    And show correct chat histories
    And preserve provider configurations

  Scenario: Chat Focus Management
    Given I have multiple chats open
    When I select a chat from the chat list
    Then focus should move to its corresponding window
    And if no window exists, a new one should open
    And the window should show the latest chat state

  Scenario: Multi-Provider Support
    Given I have configured multiple providers
    When I send a message in a chat
    Then all active providers should respond
    And responses should stream in the window
    And maintain proper formatting for each provider
    And show clear provider labels

  Scenario: Editor Chat Context Management
    Given I have a chat open in an editor
    When I send messages with code context
    Then the editor should maintain context between messages
    And show relevant code snippets
    And preserve formatting of code blocks
    And respect context window limits 