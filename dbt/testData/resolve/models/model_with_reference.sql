with customers as (

  select * from {{ ref('mod<caret>el_b') }}

)

select first_name from customers