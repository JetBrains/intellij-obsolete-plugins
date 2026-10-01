with source as (
    select
        first_name,
        last_name

    from my_table
), final as (
    select * from source
)