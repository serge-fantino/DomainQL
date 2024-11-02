# DomainQL

This is mainlky an expiriment with Cursor AI and claude 3.5 Sonnet.

The initial prompt:
I need to create a model of expression. An expression is based on a domain and can mix domain's attributes with scalar operators or aggregate operators (like sum or average). Note that attributes can be link to other domains (e.g the company domains can be linked to the people domain through attribute 'works form). An expression is actually a set (e.g the expression people.first_name is the set of all people name). A query is a special expression that can create a set of tuples as a list of expression having the same source domain. Query can also be filtered by boolen expression (quotient ?) To further restrict the resulting set. Note that a Query in itself is equivalent to a domain, where each item of the tuple can be referenced as a new attribute. 
Note that I want the Query object to be able to be traited as a Domain object too, where each query projetion is actually defining a corresponding attribute. The Query's Domain will be a new Domain, independant from teh original Domain and usually not compatible.
An finally I want a service that can transform a Query into propre SQL code. It must take into account the domains from the query to generate propre from and join statement. Also if at least one projection is an aggregate it will generate a group by for all scalar ones. And if a domain is actually from a Query that will generate sub-query.
Could you draft a simple java implementation? Please create an artifact for each class. The base package must be org.kmsf.domainql.expression

