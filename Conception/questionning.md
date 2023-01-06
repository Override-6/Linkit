
Old version of linkit was a total mess, it was badly organised, a lot of internal code was kinda ugly / did not worked properly,
was too bugged.  
In this branch we 

## Why old version was bad ?
* The fact that too many features are exposed to the user where the only interesting point is the object connection and maybe the GNOL 
  makes that linkit was in the way of becoming a bloatware.
* 

## Connected objects
* How do we handle objects that interracts with other objects that are under a different contract ? 
* We should certainly give up the fact that objects can be connected dynamically
* I think we should generate proxy classes using only javassist/bytecode manipulation libraries  
  and give up first compilation step that used scalac because it would generate practically no compilation error and be significantly faster.

## GNOM
* If a client can connect to multiple servers at once, how do we handle GNOLinkage between those two servers ? 
* 