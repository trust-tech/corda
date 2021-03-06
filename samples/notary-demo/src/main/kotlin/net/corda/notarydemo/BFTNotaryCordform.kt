package net.corda.notarydemo

import com.google.common.net.HostAndPort
import net.corda.core.div
import net.corda.core.node.services.ServiceInfo
import net.corda.core.utilities.ALICE
import net.corda.core.utilities.BOB
import net.corda.core.utilities.DUMMY_CA
import net.corda.demorun.util.*
import net.corda.demorun.runNodes
import net.corda.node.services.transactions.BFTNonValidatingNotaryService
import net.corda.node.utilities.ServiceIdentityGenerator
import net.corda.cordform.CordformDefinition
import net.corda.cordform.CordformContext
import net.corda.cordform.CordformNode
import net.corda.core.mapToArray
import net.corda.node.services.transactions.minCorrectReplicas
import org.bouncycastle.asn1.x500.X500Name

fun main(args: Array<String>) = BFTNotaryCordform.runNodes()

private val clusterSize = 4 // Minimum size thats tolerates a faulty replica.
private val notaryNames = createNotaryNames(clusterSize)

object BFTNotaryCordform : CordformDefinition("build" / "notary-demo-nodes", notaryNames[0]) {
    private val clusterName = X500Name("CN=BFT,O=R3,OU=corda,L=Zurich,C=CH")
    private val advertisedService = ServiceInfo(BFTNonValidatingNotaryService.type, clusterName)

    init {
        node {
            name(ALICE.name)
            p2pPort(10002)
            rpcPort(10003)
            rpcUsers(notaryDemoUser)
        }
        node {
            name(BOB.name)
            p2pPort(10005)
            rpcPort(10006)
        }
        val clusterAddresses = (0 until clusterSize).mapToArray { HostAndPort.fromParts("localhost", 11000 + it * 10) }
        fun notaryNode(replicaId: Int, configure: CordformNode.() -> Unit) = node {
            name(notaryNames[replicaId])
            advertisedServices(advertisedService)
            notaryClusterAddresses(*clusterAddresses)
            bftReplicaId(replicaId)
            configure()
        }
        notaryNode(0) {
            p2pPort(10009)
            rpcPort(10010)
        }
        notaryNode(1) {
            p2pPort(10013)
            rpcPort(10014)
        }
        notaryNode(2) {
            p2pPort(10017)
            rpcPort(10018)
        }
        notaryNode(3) {
            p2pPort(10021)
            rpcPort(10022)
        }
    }

    override fun setup(context: CordformContext) {
        ServiceIdentityGenerator.generateToDisk(notaryNames.map { context.baseDirectory(it) }, DUMMY_CA, advertisedService.type.id, clusterName, minCorrectReplicas(clusterSize))
    }
}
