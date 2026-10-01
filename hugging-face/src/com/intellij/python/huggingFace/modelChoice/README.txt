To register a Hugging Face library do the following:

1) Add a respective library name to [HfModelChoiceSupportedLibraries]
2) Add it in the when clause in the HfModelValidityChecker
3) Create appropriate handler in the HfLibraryTemplateHandler.kt
3) Link the library to the respective handler in the HfModelInserter
