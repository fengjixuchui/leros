
# A bit complicated handling of path and programs, a simple sbt test will run all available tests in SW and HW simulation
# TODO: cleanup

# runs all tests
test:
	sbt test

# run a single SW simulation
sw-simulation:
	sbt "runMain leros.sim.LerosSim asm/test/base.s"

APP=base
TESTS="base lhi lhi2 lognosign reg imm mem"
TESTPATH=asm/test

hwsim:
	sbt -Dprogram=$(APP) "testOnly leros.LerosTest"


swsim:
	sbt -Dprogram=$(APP) "testOnly leros.sim.LerosSimTest"

hw:
	sbt "runMain leros.Leros asm/$(APP).s"

test-alu:
	sbt "test:runMain leros.AluTester"

all: all-hwsim all-swsim

all-hwsim:
	sbt -Dtestpath=$(TESTPATH) -Dprogram=$(TESTS) "testOnly leros.LerosTest"

all-swsim:
	sbt -Dtestpath=$(TESTPATH) -Dprogram=$(TESTS) "testOnly leros.sim.LerosSimTest"

# clean everything (including IntelliJ project settings)
clean:
	git clean -fd
