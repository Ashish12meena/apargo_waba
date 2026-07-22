One service = one database schema. No service reads or writes another service's tables directly.
domain depends on nothing else in the project.
application depends only on domain.
infrastructure implements application/port/out — never the other way round.
api never touches entities or repositories directly — always goes through a use case interface.
Every table gets created_at, updated_at, deleted_at (soft delete) 
Every public endpoint is versioned