Feature: Python Integration
  As a Python developer using the AI Playground
  I want seamless integration with my Python development workflow
  So that I can efficiently use AI assistance while coding

  Background:
    Given the AI Playground plugin is installed
    And I have configured valid API credentials
    And I am using PyCharm IDE

  Scenario: API Call Detection
    Given I have a Python file with the following content:
      """
      import openai
      
      response = openai.ChatCompletion.create(
          model="gpt-4",
          messages=[{"role": "user", "content": "Hello"}]
      )
      """
    When I open the file in the editor
    Then the openai.ChatCompletion.create call should be highlighted
    And a chat action icon should appear next to the call
    And hovering should show available actions

  Scenario: Parameter Extraction
    Given I have a Python file with an LLM API call:
      """
      response = client.chat.completions.create(
          model="gpt-4",
          messages=[
              {"role": "system", "content": "You are a Python expert"},
              {"role": "user", "content": "Explain decorators"}
          ],
          temperature=0.7
      )
      """
    When I open chat from the API call
    Then the extracted parameters should be used:
      | parameter   | value                        |
      | model      | gpt-4                        |
      | system     | You are a Python expert      |
      | message    | Explain decorators           |
      | temperature| 0.7                          |

  Scenario: IDE Tool Window Integration
    Given PyCharm is running
    When the plugin is active
    Then the chat window should be docked in the IDE
    And follow the current IDE theme
    And preserve its state across IDE restarts
    And respond to IDE focus events

  Scenario: Python Context Awareness
    Given I have an open Python file with a class definition
    When I select the class code
    And open the chat window
    Then the selected code should be included in chat context
    And the chat should understand Python syntax
    And provide Python-specific assistance

  Scenario: Multiple File Context
    Given I have multiple Python files open
    When I request chat assistance
    Then the chat should understand project structure
    And recognize imports between files
    And provide context-aware suggestions

  Scenario: Quick Actions Integration
    Given I have a Python file with LLM API calls
    When I right-click on an API call
    Then I should see AI Playground actions
    And be able to quickly open chat
    And have the context pre-populated

  Scenario: Python Error Context
    Given I have a Python file with an error
    When I open chat from the error line
    Then the error context should be included
    And relevant code should be attached
    And error details should be provided

  Scenario: Project Structure Understanding
    Given I have a Python project open
    When I request project-wide assistance
    Then the chat should understand:
      | aspect            |
      | Project layout    |
      | Dependencies      |
      | Module structure  |
      | Import hierarchy  |

  Scenario: Python Documentation Support
    Given I am writing Python documentation
    When I request documentation help
    Then suggestions should follow Python conventions
    And include docstring formats
    And provide type hints
    And follow PEP style guides 