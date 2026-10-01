with customers as (

    select * from {{ ref('model_b') }}

)

select first_<caret>name from customers