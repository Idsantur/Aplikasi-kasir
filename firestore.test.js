const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: fs.readFileSync("firestore.rules", "utf8"),
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  await testEnv.cleanup();
});

test("Unauthenticated caller cannot read or write stores", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.doc("stores/081234567890").get());
  await assertFails(unauthDb.doc("stores/081234567890").set({
    phoneNumber: "081234567890",
    pinHash: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    storeName: "Toko Berkah",
    ownerUid: "some_uid",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Authenticated owner can register store with phone and PIN hash", async () => {
  const authDb = testEnv.authenticatedContext("user_abc").firestore();
  await assertSucceeds(authDb.doc("stores/081234567890").set({
    phoneNumber: "081234567890",
    pinHash: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    storeName: "Toko KasirKilat",
    ownerUid: "user_abc",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Store registration fails if ownerUid does not match caller auth uid", async () => {
  const authDb = testEnv.authenticatedContext("user_abc").firestore();
  await assertFails(authDb.doc("stores/081234567890").set({
    phoneNumber: "081234567890",
    pinHash: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    storeName: "Toko KasirKilat",
    ownerUid: "user_different",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Authenticated user can add and read products in store", async () => {
  const authDb = testEnv.authenticatedContext("user_abc").firestore();
  const prodRef = authDb.doc("stores/081234567890/products/prod_1");

  await assertSucceeds(prodRef.set({
    phoneNumber: "081234567890",
    name: "Kemeja Putih",
    barcode: "89912345",
    category: "Pakaian",
    sellingPrice: 75000,
    costPrice: 45000,
    stock: 25,
    unit: "pcs",
    isActive: true,
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  await assertSucceeds(prodRef.get());
});

test("Product creation fails if required fields or invalid types", async () => {
  const authDb = testEnv.authenticatedContext("user_abc").firestore();
  const prodRef = authDb.doc("stores/081234567890/products/prod_2");

  // Missing sellingPrice
  await assertFails(prodRef.set({
    phoneNumber: "081234567890",
    name: "Barang Tidak Lengkap",
    category: "Umum",
    costPrice: 1000,
    stock: 5,
    unit: "pcs",
    isActive: true,
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Authenticated user can add categories to store", async () => {
  const authDb = testEnv.authenticatedContext("user_abc").firestore();
  const catRef = authDb.doc("stores/081234567890/categories/cat_1");

  await assertSucceeds(catRef.set({
    phoneNumber: "081234567890",
    name: "Elektronik",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  await assertSucceeds(catRef.get());
});
