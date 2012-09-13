Handler Advice Sample "retry-and-more"
======================================

This sample shows how to use the 2.2.0 Handler Advice feature.

StatelessRetryDemo shows stateless retry.

By default, it runs with a simple default retry (3 tries, no backoff, no recovery)

Run with -Dspring.profiles.active=backoff to run with 3 tries, exponential backoff, no recovery

Run with -Dspring.profiles.active=recovery to run with 3 tries, no backoff, error message "recovery"


In each case enter 'fail n' where n is the number of times you want the service to fail.

e.g. 'fail 2' will succeed in each case on the third try, 'fail 3' will fail permanently after the third try.