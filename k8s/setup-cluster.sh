#!/bin/bash
# ===========================================
# PetBuddy - Kind Cluster Setup Script
# ===========================================

set -e

echo "🚀 Setting up PetBuddy Kind Cluster..."

# Check if kind is installed
if ! command -v kind &> /dev/null; then
    echo "❌ Kind is not installed. Please install it first:"
    echo "   https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    exit 1
fi

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed. Please install it first:"
    echo "   https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi

# Delete existing cluster if exists
echo "🗑️  Deleting existing cluster (if any)..."
kind delete cluster --name petbuddy 2>/dev/null || true

# Create cluster
echo "📦 Creating Kind cluster..."
kind create cluster --config kind-config.yaml

# Wait for cluster to be ready
echo "⏳ Waiting for cluster to be ready..."
kubectl wait --for=condition=Ready nodes --all --timeout=120s

# Install NGINX Ingress Controller
echo "🌐 Installing NGINX Ingress Controller..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# Wait for ingress controller
echo "⏳ Waiting for Ingress Controller to be ready..."
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s

# Install ArgoCD
echo "🔄 Installing ArgoCD..."
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD
echo "⏳ Waiting for ArgoCD to be ready..."
kubectl wait --namespace argocd \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/name=argocd-server \
  --timeout=300s

# Get ArgoCD admin password
echo ""
echo "✅ Cluster setup complete!"
echo ""
echo "📋 ArgoCD Admin Password:"
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
echo ""
echo ""
echo "🌐 Access ArgoCD UI:"
echo "   Run: kubectl port-forward svc/argocd-server -n argocd 8080:443"
echo "   Then open: https://localhost:8080"
echo "   Username: admin"
echo ""
echo "🔧 Next steps:"
echo "   1. Create petbuddy-gitops repository"
echo "   2. Apply ArgoCD Application manifest"
echo "   3. Push Kubernetes manifests to trigger sync"
