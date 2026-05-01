import { useQuery } from "@tanstack/react-query";
import { meApi } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export function LandingPage() {
  const { logout } = useAuth();
  const { data, isLoading, error } = useQuery({
    queryKey: ["me"],
    queryFn: meApi.get,
  });

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-xl">
        <CardHeader className="flex-row items-start justify-between space-y-0">
          <div className="space-y-1.5">
            <CardTitle>Welcome back</CardTitle>
            <CardDescription>Your account info, fetched from the API.</CardDescription>
          </div>
          <Button variant="outline" onClick={() => void logout()}>
            Sign out
          </Button>
        </CardHeader>
        <CardContent>
          {isLoading && <p className="text-muted-foreground">Loading…</p>}
          {error && <p className="text-destructive">Could not load your profile.</p>}
          {data && (
            <dl className="grid grid-cols-3 gap-4 text-sm">
              <dt className="text-muted-foreground">Display name</dt>
              <dd className="col-span-2 font-medium">{data.displayName}</dd>
              <dt className="text-muted-foreground">Email</dt>
              <dd className="col-span-2 font-medium">{data.email}</dd>
              <dt className="text-muted-foreground">Member since</dt>
              <dd className="col-span-2 font-medium">
                {new Date(data.memberSince).toLocaleString()}
              </dd>
            </dl>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
