def _detect_outliers(df,features, multiplier, threshold):
    import numpy as np
    from collections import Counter

    outlier_indices = []

    for c in features:
        # 1st quartile
        Q1 = np.percentile(df[c],25)
        # 3rd quartile
        Q3 = np.percentile(df[c],75)
        # IQR
        IQR = Q3 - Q1
        # Outlier step
        outlier_step = IQR * multiplier
        # detect outlier and their indeces
        outlier_list_col = df[(df[c] < Q1 - outlier_step) | (df[c] > Q3 + outlier_step)].index
        # store indeces
        outlier_indices.extend(outlier_list_col)

    outlier_indices = Counter(outlier_indices)
    multiple_outliers = list(i for i, v in outlier_indices.items() if v > threshold)

    return multiple_outliers


def drop_outliers(df, columns, multiplier, threshold = 2):
    """
    :display-name Drop Outliers
    :description Drop a bunch of outliers
    :label Drop outliers in {columns}
    :details Drop outliers in {columns} (more than {threshold})
    :param df:
    :param columns: :display-name In columns :type set(column)
    :param multiplier: :display-name Multiplier :type float(1.5, 1, 2, 3)
    :param threshold: :display-name Outliers threshold :type int
    :return:
    """
    return df.drop(_detect_outliers(df, columns, multiplier, threshold), axis=0).reset_index(drop=True)

def project(df, columns):
    """
    :display-name Project
    :description Extract a bunch of columns
    :label Project on {columns}
    :param df:
    :param columns: :type set(column)
    :return:
    """
    return df[columns]

def group_by(df, columns, operation):
    """
        :display-name Group By
        :description Group by columns and compute operation
        :label Group by {columns} and compute {operation}
        :param df:
        :param columns: :display-name Columns :type set(column)
        :param operation: :type string(mean, sum, min, max, count)
        :return:
        """
    return getattr(df.groupby(columns, as_index = False), operation)()

def sort_values(df, column, order='ascending'):
    """
        :display-name Sort Values
        :description Sort values
        :label Sort by {column} {order}
        :param df:
        :param column: :display-name Column :type column
        :param order: :type string(ascending, descending)
        :return:
        """
    return df.sort_values(by=column, ascending=(order=='ascending'))