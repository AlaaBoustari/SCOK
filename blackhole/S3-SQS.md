New concepts Learned :

## @DynamicUpdate

you can avoid updating the entier entity on the UPDATE statement and only modify the dirty fields


## QUeue time

The retention time actually counts both time spent in the queue and DLQ. meaning if you set up 4 day retention policy,
and 1 day elapsed in the QUEUE before being passed to a DLQ. you now only have 3 days left. SHOCKER.
