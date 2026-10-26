Strategic DDD: where are the borders of meaning?

Tactical / implementation: how organize code and behavior inside those borders?

Below is a compact “concept map” + mental model.

1. Bounded Context

What it is

A semantic boundary around a specific domain model and its ubiquitous language.

Inside it, terms are precise; outside, the same words may mean something else.

Mental model

“A mini-system inside the system with its own rules and language.”

Examples: Sales, Billing, Shipping, Identity.

Role in the map

Top-level container for everything else: modules, aggregates, use cases, slices.

2. Module

What it is

A technical grouping of code within a bounded context.

A module is about structure, not necessarily directly about business behavior.

Examples of module meanings:

Package / namespace: com.company.sales.orders

Deployment module: JAR, Gradle module, Maven module.

Logical sub-area: pricing, catalog, invoicing.

Mental model

“Folders/boxes inside the bounded context for keeping things ordered.”

You can have modules by subdomain (pricing, orders) or by layers (domain, application, infra).

Role in the map

Inside a bounded context you use modules to structure:

Aggregates,

Use cases (execution units),

Technical infrastructure.

3. Aggregate

What it is

A cluster of domain objects treated as a single consistency boundary.

Has a root (Aggregate Root) that:

owns invariants,

is the only entry point for modifications,

is what repositories load/save.

Examples: Order, Invoice, Customer, Contract.

Mental model

“A small transactional island of business rules.”

One aggregate = one transactional consistency boundary.

Role in the map

Lives inside a bounded context, typically inside a domain module, e.g.:

com.company.sales.domain.order.Order

Used by execution units (use cases) to perform business operations.

Vertical slice usually “touches” one or a small number of aggregates.

4. Execution Unit (Use Case)

What it is

A single, complete business operation executed by the system.

Has:

input (command/query),

application/service logic (handler),

calls to domain (aggregates) and ports,

output (response, event, result).

Examples: PlaceOrder, CancelOrder, IssueInvoice, RegisterUser.

Mental model

“One button the business cares about.”

If a business person says: ‘When user does X, system should do Y’ → you are probably looking at an execution unit.

Role in the map

Lives in the Application Layer of a bounded context.

Coordinates:

aggregates,

domain services,

repositories / external systems (via ports).

5. Vertical Slice

What it is

The full end-to-end implementation of one execution unit across layers:

API / UI entry (controller, endpoint, message handler),

application/use case handler (execution unit),

domain (aggregates, domain logic, events),

ports and adapters (repositories, clients).

Mental model

“A thin vertical slice of the cake: from top (API) to bottom (DB/external systems) for one use case.”

In code: usually a feature folder or package per use case.

Role in the map

A vertical slice implements one execution unit inside one bounded context.

It may be represented as a module (or submodule) itself, especially in a modular monolith.

6. Putting it all together – mental model

Picture it as a hierarchy:

System
└── Bounded Context (e.g. Sales)
├── Modules (technical grouping)
│     ├── Domain module (e.g. domain.orders)
│     │     └── Aggregates (Order, OrderLine, etc.)
│     ├── Application module (e.g. application.orders)
│     │     └── Execution Units / Use Cases
│     └── Infrastructure module (e.g. infra.orders)
└── Vertical Slices
├── PlaceOrder slice
│    ├─ API: PlaceOrderController
│    ├─ Application: PlaceOrderCommand, PlaceOrderHandler
│    ├─ Domain: Order aggregate (+ domain logic it uses)
│    └─ Infra: OrderRepository implementation, event publisher, etc.
└── CancelOrder slice
...


Or in short:

Bounded context – business language boundary.

Module – technical grouping inside that context.

Aggregate – consistency boundary inside the domain model.

Execution unit / Use case – one business operation.

Vertical slice – everything needed to run that one operation, across layers, often organized as its own package/module.