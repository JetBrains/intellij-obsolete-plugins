Feature: Settings and Configuration
  As a developer using the AI Playground
  I want to configure the plugin settings
  So that I can customize the behavior and manage API access

  Background:
    Given the AI Playground plugin is installed
    And I am in the IDE settings window
    And I navigate to the AI Playground settings

  Scenario: Configure API credentials
    When I enter valid API credentials for a provider
    And I click the Save button
    Then the credentials should be securely stored
    And I should see a confirmation message
    And the provider should become available

  Scenario: Validate API credentials
    When I enter invalid API credentials
    And I click the Save button
    Then I should see an error message
    And the invalid credentials should not be saved
    And the provider should remain unavailable

  Scenario: Manage multiple providers
    Given I have configured multiple providers
    When I view the providers list
    Then I should see all configured providers
    And each provider's status should be displayed
    And I should be able to enable/disable providers

  Scenario: Configure model preferences
    Given a provider is properly configured
    When I select specific models to enable
    Then only selected models should be available
    And the changes should persist after restart
    And disabled models should not appear in chat

  Scenario: Import/Export settings
    Given I have configured plugin settings
    When I export the settings
    Then a settings file should be created
    And I should be able to import these settings
    And all configurations should be preserved

  Scenario: Customize UI behavior
    When I modify UI settings
    Then the changes should apply immediately
    And the chat interface should reflect changes
    And settings should persist between sessions

  Scenario: Handle provider-specific settings
    Given a provider requires specific configuration
    When I configure provider-specific settings
    Then these settings should be validated
    And stored separately from other providers
    And applied only to the specific provider

  Scenario: Manage rate limits
    Given I have configured rate limiting
    When I reach the configured limit
    Then requests should be throttled
    And I should see a notification
    And normal operation should resume after delay

  Scenario: Reset settings
    When I click the Reset to Defaults button
    Then all settings should return to defaults
    And I should see a confirmation prompt
    And previous settings should be backed up

  Scenario: Theme integration
    Given I change the IDE theme
    When I open the chat window
    Then the chat UI should match the theme
    And all components should be properly styled
    And text should remain readable 