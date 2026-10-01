# custom.multi_outliers.drop_outliers
* name = Drop Outliers
* description = Drop a bunch of outliers
* label = Drop outliers in []
* details = Drop outliers in [] (more than 2)
* drop_outliers
  * columns
    * name = In columns
    * type = Set
    * def = []
    * dwtype = coll Set (def DWTableColumn)
    * values = null
  * multiplier
    * name = Multiplier
    * type = Float
    * def = 1.5
    * dwtype = fake Float
    * values = [1.5, 1.0, 2.0, 3.0]
  * threshold
    * name = Outliers threshold
    * type = Int
    * def = 2
    * dwtype = fake Int
    * values = null
# custom.multi_outliers.project
* name = Project
* description = Extract a bunch of columns
* label = Project on []
* details = 
* project
  * columns
    * name = columns
    * type = Set
    * def = []
    * dwtype = coll Set (def DWTableColumn)
    * values = null
# custom.multi_outliers.group_by
* name = Group By
* description = Group by columns and compute operation
* label = Group by [] and compute mean
* details = 
* group_by
  * columns
    * name = Columns
    * type = Set
    * def = []
    * dwtype = coll Set (def DWTableColumn)
    * values = null
  * operation
    * name = operation
    * type = String
    * def = mean
    * dwtype = fake String
    * values = [mean, sum, min, max, count]
# custom.multi_outliers.sort_values
* name = Sort Values
* description = Sort values
* label = Sort by  ascending
* details = 
* sort_values
  * column
    * name = Column
    * type = String
    * def = 
    * dwtype = anno DWTableColumn
    * values = []
  * order
    * name = order
    * type = String
    * def = ascending
    * dwtype = fake String
    * values = [ascending, descending]